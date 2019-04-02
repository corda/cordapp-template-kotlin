<p align="center">
  <a href="https://ibb.co/KyPL2rt"><img src="https://i.ibb.co/2K9svkz/Git-Coinsv3.png" alt="Git-Coinsv3" border="0" /></a>
</p>

# GitCoins CorDapp

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

Download [ngrok](https://ngrok.com/download) to expose your local end points to the web. 

# Usage

From the root directory run the following commands:

* `./gradlew clean deployNodes`
* `build/nodes/runnodes`

Once built, start the spring boot web server [Server.kt](https://github.com/willhr3/review-tokens-cordapp/blob/release-V4/clients/src/main/kotlin/com/gitcoins/webserver/Server.kt)

Navigate to your ngrok installation and run the following command 
* `./ngrok http 8080`

Copy the forwarding address.

## GitHub Webhook Configuration

Navigate to the repository that you would like GitCoins to be rewarded for contributions. Configure the following webhooks via `Settings > Webhooks > Add webhook`:
* `pull_request_review_comments`

  * payload URL: <ngrok forwarding address>/api/git/create-key
  
  * content type: JSON

* `push`
  
  * payload URL: <ngrok forwarding address>/api/git/push-event

  * content type: JSON
  
* `pull_request`
  
  * payload URL: <ngrok forwarding address>/api/git/pr-event
  
  * content type: JSON

## Issuing GitCoins

To generate a key for a GitHub user you will need first open a pull request on the repo. Once open, the user must comment 'createKey' on a portion of the unified diff. (The GitHub Review Comments API is desribed [here](https://developer.github.com/v3/pulls/comments/#list-comments-on-a-pull-request))

Now the user is linked to an `AnonymousParty` they will be issued 1 GitCoin for each push, or pull request on the repo. 
