const express = require("express");
const axios = require("axios");
const querystring = require("querystring");
const puppeteer = require("puppeteer");
const port = 8081;
const ssoUrl = "https://sso.bps.go.id/auth/realms/eksternal/protocol/openid-connect/auth?client_id=03310-icscapi-k09&redirect_uri=http://localhost:" + port + "/callback&response_type=code"
const app = express();
app.use(express.json())
app.use(express.static("callback"));
const server = require("http").createServer(app);

server.listen(port, () => {
  console.info("Server is running at port " + port);
});

app.post("/sso", async (req, res) => {
  try {
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
    await page.goto(ssoUrl, { waitUntil: "domcontentloaded" });
    await page.type('#username', req.body.username);
    await page.type('#password', req.body.password);
    await page.click('#kc-login');
    await page.content();
    innerText = await page.evaluate(() => {
      return JSON.parse(document.querySelector("body").innerText);
    });
    res.json({
      success: true,
      message: "Logged in successfully.",
      data: innerText
    })
  } catch (e) {
    res.json({
      success: false,
      message: "Error occured. [" + e.message + "]"
    }).status(500)
  }
})

app.get("/callback/", async (req, res) => {
  try {
    const { code } = req.query;
    console.info("Authorization Code: " + code + "\n");
    const response = await axios
      .post(
        "https://sso.bps.go.id/auth/realms/eksternal/protocol/openid-connect/token",
        querystring.stringify({
          client_id: "03310-icscapi-k09",
          grant_type: "authorization_code",

          redirect_uri: "http://localhost:" + port + "/callback",
          code: code,
        })
      )
    res.json(response.data)
  } catch (e) {
    res.json({
      success: false,
      message: "Error occured. [" + e.message + "]"
    }).status(500)
    console.error(error);
  }
});