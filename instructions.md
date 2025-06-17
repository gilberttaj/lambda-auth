## LambdaAuth

### 

1. Create a lambda function api gateway to handle aws cognito Google SSO authentication using sam cli using java.
2. Amazon API Gateway should be created with Private API
3. I already created all the vpc, subnets, vpc endpoints for lambda, and security groups and internet gateway, natgateway, route tables etc.
4. make sure that connections cannot be made from non-VPN sources.
5. Cognito should work by using Amazon API Gateway (Private API) as Proxy.
6. Login with google is being triggered from the client side. (also private network)
7. When authenticating with Cognito, implement a function that starts Lambda, checks the email, and only allows certain emails to authenticate.
This is to ensure that only people in some departments in the account registered in Google Work Space can log in.
8. I have already created a user pool and already set up the google sso.
9. All you need to do is setup the lambda function to handle the authentication.
10. Please do the steps one at a time and make sure all steps are covered.