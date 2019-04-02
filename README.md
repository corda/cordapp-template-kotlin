<p align="center">
  <a href="https://ibb.co/5jMDpRd"><img src="https://i.ibb.co/2Mn2Vkw/GitCoins.png" alt="GitCoins" border="0" /></a>
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


## Interacting with the nodes:

