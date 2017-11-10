# AWS Lambda & Vertx Framework URL Shortener Backend

This is a simple **Vertx** project deployed as **AWS Lambda**. It demonstrates how easy it is to create, manage and deploy a truly serverless micro service. Using the Serverless Framework deploying and managing was never so easy.

Project is a url shortening service, like tinyurl, bitly... It relies on Vertx Async Api and AWS Lambda service.

## API endpoints
- URL: `POST /api`
  - Params type: `json`
  - Required Body Params: `longurl`
  - Output: `shorturl`

- URL: `GET /{shorturl}`
  - Required Path Params: `shorturl`
  - Output: `redirect to longurl`

## Deploying and Running
To run/deploy this example you will need to install [Serverless](https://serverless.com)  
Open serverless.env.yml and change JDBC parameters.  
After you are done here are the necceserry commands.
```sh
$ mvn clean package
$ serverless deploy -v
```
After this you should have your Lambda service up and running.
You can find a blog post about this example [here](http://lazarbulic.com/blog/?p=154&preview=true).

