/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seyren.core.service.notification;

import com.seyren.core.domain.*;
import com.seyren.core.exception.NotificationFailedException;
import com.seyren.core.util.config.SeyrenConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * User: ellios
 * Time: 14-3-13 : 下午5:35
 */
@Named
public class SmsNotificationService implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationService.class);
    private final SeyrenConfig seyrenConfig;

    @Inject
    public SmsNotificationService(SeyrenConfig seyrenConfig) {
        this.seyrenConfig = seyrenConfig;
    }

    @Override
    public void sendNotification(Check check, Subscription subscription, List<Alert> alerts) throws NotificationFailedException {
        String smsUrl = seyrenConfig.getSmsUrl();
        if (StringUtils.isEmpty(smsUrl)) {
            LOGGER.warn("sms url is empty. can not send notification by sms");
            return;
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", seyrenConfig.getSmsUsername()));
        params.add(new BasicNameValuePair("userkey", seyrenConfig.getSmsUserKey()));
        params.add(new BasicNameValuePair("tolist", subscription.getTarget()));
        params.add(new BasicNameValuePair("content", createContent(check)));
        params.add(new BasicNameValuePair("type", "warn"));


        HttpClient client = HttpClientBuilder.create().build();

        HttpPost post = new HttpPost(smsUrl);
        try {
            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = client.execute(post);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                LOGGER.info("Sms Response : {} ", EntityUtils.toString(responseEntity));
            }
        } catch (Exception e) {
            throw new NotificationFailedException("Failed to send notification to HTTP", e);
        } finally {
            post.releaseConnection();
            HttpClientUtils.closeQuietly(client);
        }
    }

    private String createContent(Check check) {
        if (check.getState() == AlertType.ERROR) {
            return format("[CRITICAL] | Please check %s", check.getName());
        }
        if (check.getState() == AlertType.WARN) {
            return format("[WARN] | Please check %s", check.getName());
        }
        if (check.getState() == AlertType.OK) {
            return format("[OK] | %s", check.getName());
        }

        LOGGER.info("Unmanaged check state [%s] for check [%s]", check.getState(), check.getName());
        return "";
    }

    @Override
    public boolean canHandle(SubscriptionType subscriptionType) {
        return subscriptionType == SubscriptionType.SMS;
    }
}
