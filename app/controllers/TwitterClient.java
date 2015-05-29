package controllers;

import play.api.Logger;
import twitter.ITwitterClient;
import twitter.TwitterStatusMessage;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TwitterClient implements ITwitterClient{

    public TwitterClient() {

    }

    @Override
    public void publishUuid(TwitterStatusMessage message) throws Exception {

        TwitterFactory factory = new TwitterFactory();
        AccessToken accessToken = new AccessToken("1366513208-MutXEbBMAVOwrbFmZtj1r4Ih2vcoHGHE2207002","RMPWOePlus3xtURWRVnv1TgrjTyK7Zk33evp4KKyA");
        Twitter twitter = factory.getInstance();
        twitter.setOAuthConsumer("GZ6tiy1XyB9W0P4xEJudQ","gaJDlW0vf7en46JwHAOkZsTHvtAiZ3QUd2mD1x26J9w");
        twitter.setOAuthAccessToken(accessToken);

        Status stat = twitter.updateStatus(message.getTwitterPublicationString());

    }

}
