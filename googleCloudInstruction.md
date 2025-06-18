# Google Cloud Console (Google Workspace) OAuth 2.0 Configuration

This guide details how to set up an OAuth 2.0 Client ID and Secret in the Google Cloud Console. These credentials are required by AWS Cognito to configure Google as a federated identity provider.

**Prerequisites:**
*   Access to Google Cloud Console (`console.cloud.google.com`).
*   A Google Cloud Project. If you don't have one, you'll need to create it.
*   Appropriate permissions to create OAuth consent screens and credentials (e.g., Project Owner, Editor, or a custom role with relevant IAM permissions).

**Steps:**

**Part 1: Configure OAuth Consent Screen**

If you haven't already configured an OAuth consent screen for your project, you must do this first.

1.  **Navigate to APIs & Services > OAuth consent screen:**
    *   In the Google Cloud Console, open the navigation menu (hamburger icon).
    *   Go to "APIs & Services" > "OAuth consent screen."

2.  **Choose User Type:**
    *   **Internal:** If your application is only for users within your Google Workspace organization. This simplifies the verification process and makes the app available immediately to organization users.
    *   **External:** If your application can be used by any Google user. 
        *   When first created, an "External" app defaults to a **"Testing" publishing status**. In this status, only explicitly added **Test Users** (see step 7) can use the app.
        *   To allow all Google users, the app must eventually be **"Published to production"**. This may require app verification by Google, especially if you use sensitive scopes (though typically not an issue for basic `email`, `profile`, `openid` used by Cognito).
    *   Select the appropriate user type (e.g., "External" for testing with specific Google accounts initially) and click "Create."

3.  **App Information:**
    *   **App name:** Enter a user-facing name for your application (e.g., "MyCompany LambdaAuth App"). This will be shown to users during the Google sign-in flow.
    *   **User support email:** Select your email address or a support email.
    *   **App logo (Optional):** Upload a logo.

4.  **Developer Contact Information:**
    *   Enter one or more email addresses for Google to contact you with updates about your project.

5.  **Click "Save and Continue."**

6.  **Scopes (Optional for this step, can be configured later or left default):**
    *   For Cognito integration, you typically need `email`, `profile`, and `openid`. Cognito will request these. You don't usually need to add them explicitly here unless you have other specific needs.
    *   Click "Save and Continue."

7.  **Test Users (Required for 'External' apps in 'Testing' status):**
    *   If you selected "External" for the user type, your app will initially be in a **"Testing" publishing status**. 
    *   During this phase, you **must** add the Google account email addresses of users who need to test the Google sign-in functionality. Only these registered test users will be able to authenticate via Google.
    *   Click "+ ADD USERS", enter their email addresses, and then click "Save and Continue."

8.  **Summary:**
    *   Review the summary. 
    *   Click "Back to Dashboard". From the dashboard, you can manage your consent screen, including adding more test users or eventually preparing to publish the app.
    *   For **Internal** apps, it's typically available to your organization users immediately.
    *   For **External** apps in **Testing** status, only test users can access it. To make it available to all Google users, you would later need to go through the "Publish App" process from the OAuth consent screen dashboard (this may involve a verification step by Google, depending on the scopes used).

**Part 2: Create OAuth 2.0 Client ID**

1.  **Navigate to APIs & Services > Credentials:**
    *   In the Google Cloud Console, open the navigation menu.
    *   Go to "APIs & Services" > "Credentials."

2.  **Create Credentials:**
    *   Click on "+ CREATE CREDENTIALS" at the top of the page.
    *   Select "OAuth client ID" from the dropdown.

3.  **Configure OAuth Client ID:**
    *   **Application type:** Select "Web application."
    *   **Name:** Give your OAuth client ID a name (e.g., "Cognito LambdaAuth Web Client"). This is for your reference in the console.

4.  **Authorized JavaScript origins (Not typically needed for Cognito server-side federation):**
    *   This is usually for client-side JavaScript applications directly calling Google APIs. For Cognito federation, Cognito handles the interaction. You can generally leave this blank.

5.  **Authorized redirect URIs:**
    *   This is **CRITICAL**. This URI is where Google will redirect users after they have successfully authenticated.
    *   This URI must be your **Amazon Cognito domain callback URL**.
    *   The format is: `https://<your-cognito-domain>/oauth2/idpresponse`
    *   Replace `<your-cognito-domain>` with your actual Cognito User Pool domain. You can find this in your Cognito User Pool settings under "Domain name" (e.g., `myapp.auth.us-east-1.amazoncognito.com`).
    *   Example: `https://my-lambda-auth.auth.us-east-1.amazoncognito.com/oauth2/idpresponse`
    *   Click "+ ADD URI" and enter this URL.

6.  **Click "CREATE."**

7.  **Collect Your Credentials:**
    *   A dialog box will appear showing your **Client ID** and **Client Secret**.
    *   **COPY THESE VALUES IMMEDIATELY AND STORE THEM SECURELY.** You will need them to configure Google as an identity provider in your AWS Cognito User Pool.
    *   The Client Secret, in particular, is sensitive.
    *   You can also download the credentials as a JSON file.

**Important Notes:**
*   **Security:** Keep your Client Secret confidential. Do not embed it in client-side code or commit it to version control. Cognito will store it securely.
*   **Verification & Publishing Status:** If you chose "External" for the user type, your app starts in a "Testing" publishing status, accessible only by designated test users. To allow all Google users, you'll need to "Publish" the app, which might involve a verification process by Google if you use sensitive scopes (beyond basic `profile`, `email`, `openid`). For standard Cognito integration with these basic scopes, verification is often straightforward or not required, but the app still needs to be explicitly published to move out of the "testing" state if you want general availability.
*   **Enable APIs:** Ensure the "Google People API" (or older "Google+ API" if still referenced in some docs, though People API is current) is enabled for your project. It's usually enabled by default when you create OAuth credentials that would use it. You can check this under "APIs & Services" > "Library."

You now have the Google Client ID and Client Secret needed for the AWS Cognito configuration.
