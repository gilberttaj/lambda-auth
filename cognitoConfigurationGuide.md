# AWS Cognito User Pool: Google Federation Configuration

This guide explains how to manually configure an AWS Cognito User Pool to allow users to sign in with their Google accounts.

**Prerequisites:**
*   An existing AWS Cognito User Pool. If you don't have one, create it first.
*   **Google Client ID** and **Client Secret** obtained from the Google Cloud Console (as per `googleCloudInstruction.md`).
*   Access to the AWS Management Console with permissions to modify Cognito User Pools.

**Steps:**

1.  **Navigate to your Cognito User Pool:**
    *   Open the AWS Management Console.
    *   Go to the Amazon Cognito service.
    *   Select "User Pools" and click on the User Pool you want to configure.

2.  **Configure Identity Providers:**
    *   In the left-hand navigation pane of your User Pool, under "Sign-in experience", click on "Federated identity provider sign-in" (or similar wording like "Identity providers" under "Federation" in older UIs).

3.  **Add Google as an Identity Provider:**
    *   Under the "Social identity providers" section (or a general list of IdPs), select "Google."
    *   You will see fields to configure Google:
        *   **Google app ID:** Enter the **Client ID** you obtained from the Google Cloud Console.
        *   **App secret:** Enter the **Client Secret** you obtained from the Google Cloud Console.
        *   **Authorize scope:** This defines what information Cognito will request from Google. The default `profile email openid` is usually sufficient.
            *   `profile`: Access to basic profile information.
            *   `email`: Access to the user's email address.
            *   `openid`: Required for OpenID Connect.
        *   Click "Enable Google" or "Add Google" (button text may vary).

4.  **Configure Attribute Mapping:**
    *   After enabling Google, you need to map attributes from Google to your Cognito User Pool attributes. This ensures that when a user signs in with Google, their information (like email, name) is correctly populated in the Cognito user profile.
    *   Go to "Attribute mapping" under the "Federation" or "Sign-in experience" section in the left navigation pane.
    *   You will see a tab or section for Google.
    *   Map the Google attributes to your Cognito User Pool attributes. Common mappings:
        *   **Google `email`** -> **Cognito `Email`** (This is crucial for your Lambda's email domain validation).
        *   **Google `given_name`** -> **Cognito `Given name`** (or `name` if you prefer full name in one field and Google provides it that way).
        *   **Google `family_name`** -> **Cognito `Family name`**.
        *   **Google `picture`** -> **Cognito `Picture`**.
        *   **Google `sub` (Subject - unique ID from Google)** -> You might map this to a custom attribute in Cognito if you need to store Google's unique ID for the user, or Cognito might handle user linking automatically based on email. Often, `Username` in Cognito is set to be the `sub` from the IdP.
    *   Ensure you have the corresponding attributes (e.g., `email`, `given_name`, `family_name`) enabled as standard or custom attributes in your User Pool settings (under "Sign-up experience" > "User attributes").
    *   Mark any attributes as "Writable" in Cognito if you want them to be updated from Google during sign-in.
    *   Click "Save changes."

5.  **Configure App Client Settings:**
    *   In the left-hand navigation, go to "App integration" > "App client settings."
    *   Select the App client(s) that your Vue.js application will use.
    *   Under "Enabled Identity Providers," make sure "Google" is checked.
    *   **Callback URL(s):**
        *   These are the URLs where Cognito will redirect the user after they sign in or sign out through your application.
        *   Example: `http://localhost:8080/callback` (for local Vue development), `https://your-vue-app.com/callback`.
        *   Ensure these match what you configure in your Amplify setup (`redirectSignIn`).
    *   **Sign out URL(s):**
        *   Example: `http://localhost:8080/logout`, `https://your-vue-app.com/logout`.
        *   Ensure these match `redirectSignOut` in Amplify.
    *   **Allowed OAuth Flows:**
        *   Typically "Authorization code grant" should be checked if you are using the Cognito Hosted UI and `responseType: 'code'`.
        *   "Implicit grant" can be checked if you need `responseType: 'token'`, but code grant is generally more secure.
    *   **Allowed OAuth Scopes:**
        *   Ensure `email`, `openid`, `profile`, and `aws.cognito.signin.user.admin` are checked.
    *   Click "Save changes."

6.  **Configure Domain Name:**
    *   If you haven't already, you need a domain name for your User Pool. This domain hosts the Cognito Hosted UI.
    *   In the left-hand navigation, go to "App integration" > "Domain name."
    *   You can use an Amazon Cognito domain (e.g., `your-prefix.auth.your-region.amazoncognito.com`) or a custom domain.
    *   Enter your desired prefix and check availability. Click "Save changes."
    *   **Note this domain name.** It's used in your Google Cloud Console "Authorized redirect URIs" (`https://<your-cognito-domain>/oauth2/idpresponse`) and in your Vue app's Amplify configuration (`oauth.domain`).

7.  **(Optional) Customize Hosted UI:**
    *   Under "App integration" > "Hosted UI customization," you can customize the look and feel of the sign-in pages if you wish.

**Verification:**
*   Once configured, you should be able to use the "Launch Hosted UI" link (found in "App client settings" or "Domain name" sections) to test the Google Sign-In flow.
*   When you click to sign in with Google, you should be redirected to Google, and after successful authentication, back to Cognito, and then to your application's callback URL.
*   Check the user's profile in the Cognito User Pool ("Users" tab) to ensure attributes are correctly mapped from Google.

This completes the manual configuration of your Cognito User Pool for Google federation. Your Vue.js application, using AWS Amplify, should now be able to initiate the Google sign-in flow through Cognito.
