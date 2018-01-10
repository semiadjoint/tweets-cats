# tweets
tweet processing

## trying it out

If you want to take this for a spin, update the twitter config values with your own. The `twitter.consumer-key`, `twitter.token`, and `twitter.secrets-file` values are non-secret and are expected in `resources/default.cfg`. The `twitter.consumerSecret` and `twitter.tokenSecret` values are secret and are expected in the file indicated by `twitter.secrets-file`. `twitter.secrets-file` should be a path relative to the `resources/` directory. The `resources/secrets/` directory is already `.gitignore`d, so that's a handy place to put it. In retrospect, this scheme is a little inflexible and could be simplified. 

The app lives in the `app` project. To start the app using sbt-revolver, run `app/reStart` at the sbt prompt. The app will connect to twitter and begin processing the incoming stream of tweets. A background polling job will log the current message statistics data to your console with a cadence of `poller.cadence`. You can configure the "top K" shown by twiddling `poller.top-k-size` to your liking. Output should look something like this:
```
app [pool-1-thread-3] INFO  c.e.t.app.App - Summary(
app   totalMessageCount=17220,
app   emojiMessageCount=210,
app   urlMessageCount=4124,
app   photoUrlMessageCount=70,
app   topEmoji=List((SPARKLES,121,true), (HEAVY EXCLAMATION MARK SYMBOL,34,true), (WHITE HEAVY CHECK MARK,32,true), (RAISED HAND,27,true)),
app   topHashtags=List((#OTGala10,151,true), (#TengoHartasGanasD,72,true), (#BTC,49,false), (#Movie,49,false)),
app   topUrlDomains=List((twitter.com,1870,true), (du3a.org,267,true), (bit.ly,178,true), (youtu.be,146,true)),
app   topPhotoUrlDomains=List((instagram.com,69,true), (pic.twitter.com,1,true), (,0,false)),
app   averageHour=135349,
app   averageMin=2256,
app   averageSec=38
app )
```

If you're in the mood for JSON, you can query the current message stats at will by hitting the `/tweetstat/stats/<k>` endpoint, where `<k>` should be the desired top K for the stats. Http is served at localhost on `server.port` port by default. The output should look something like:
```json
#~/x C:0 $ http :8080/tweetstat/stats/2
HTTP/1.1 200 OK
Content-Length: 439
Content-Type: application/json
Date: Mon, 08 Jan 2018 23:26:05 GMT

{
    "averageHour": 135219,
    "averageMin": 2254,
    "averageSec": 38,
    "emojiMessageCount": 313,
    "photoUrlMessageCount": 110,
    "topEmoji": [
        [
            "SPARKLES",
            186,
            true
        ],
        [
            "WHITE HEAVY CHECK MARK",
            55,
            true
        ]
    ],
    "topHashtags": [
        [
            "#OTGala10",
            227,
            true
        ],
        [
            "#TengoHartasGanasD",
            97,
            true
        ]
    ],
    "topPhotoUrlDomains": [
        [
            "instagram.com",
            109,
            true
        ],
        [
            "pic.twitter.com",
            1,
            true
        ]
    ],
    "topUrlDomains": [
        [
            "twitter.com",
            2907,
            true
        ],
        [
            "du3a.org",
            390,
            true
        ]
    ],
    "totalMessageCount": 26449,
    "urlMessageCount": 6308
}
```

## questions and gripes
If you've got questions or want to complain about something, feel free to create a github issue.

## some todos
- handle retries
- handle non-ascii hashtags (use `entities` like we do with urls)
- add some specs
- prettier console output
