# Vue.js Frontend - Key Next Steps

This document outlines the essential steps to get your Vue.js frontend client up and running after the initial project scaffolding.

The frontend is located in the `front/` directory.

## 1. Install Dependencies

Before you can run the application, you need to install the necessary Node.js packages defined in `package.json`.

*   Open your terminal or command prompt.
*   Navigate to the frontend directory:
    ```bash
    cd path/to/your/project/GREAMORB/LambdaAuth/front
    ```
*   Run the installation command:
    ```bash
    npm install
    ```
    (Or, if you use Yarn: `yarn install`)

## 2. Configure AWS Amplify

AWS Amplify needs to be configured with your specific AWS Cognito User Pool and App Client details. This configuration tells Amplify how to connect to your backend authentication services.

*   Open the file: `front/amplifyconfiguration.json`
*   You will see placeholder values (e.g., `YOUR_AWS_REGION`, `YOUR_COGNITO_USER_POOL_ID`).
*   **Replace these placeholders with your actual AWS resource details:**
    *   `"aws_project_region"`: Your AWS Region (e.g., `"us-east-1"`).
    *   `"aws_cognito_region"`: Your AWS Region (e.g., `"us-east-1"`).
    *   `"aws_user_pools_id"`: Your Cognito User Pool ID (e.g., `"us-east-1_xxxxxxxxx"`).
    *   `"aws_user_pools_web_client_id"`: Your Cognito App Client ID (e.g., `"xxxxxxxxxxxxxxxxxxxxxxxxxx"`).
    *   `"oauth.domain"`: Your Cognito User Pool domain (e.g., `"your-app-name.auth.your-region.amazoncognito.com"`).
    *   `"oauth.redirectSignIn"`: The URL where Cognito should redirect after a successful sign-in. For local development with Vite (which is set up in `package.json`), this is typically `http://localhost:5173/`. If you deploy your app, change this to your deployed app's URL.
    *   `"oauth.redirectSignOut"`: The URL where Cognito should redirect after a sign-out. This can also be `http://localhost:5173/` or a specific logout page like `http://localhost:5173/logout/`.
    *   **Note:**
        *   If you are *not* using Cognito Identity Pools for unauthenticated access (which is the case for this primary Google SSO flow), you can remove the `"aws_cognito_identity_pool_id"` line.
        *   If you are *not* using AWS AppSync, you can remove the AppSync-related keys (`"aws_appsync_graphqlEndpoint"`, `"aws_appsync_region"`, `"aws_appsync_authenticationType"`).

## 3. Configure API Endpoint for Lambda

The `Login.vue` component makes a POST request to your private API Gateway endpoint (`/auth`) after a successful sign-in to validate the ID token with your Lambda function. Amplify needs to know the URL of this API.

*   This API Gateway URL will only be available **after** you deploy your backend SAM application.
*   Once you have the Invoke URL for your private API Gateway, update the Amplify configuration.

    You have two main options:

    **Option A: Update `amplifyconfiguration.json` (Recommended)**
    Add an `API` section to your `front/amplifyconfiguration.json` file:
    ```json
    {
        // ... your existing Cognito configurations ...
        "API": {
            "endpoints": [
                {
                    "name": "PrivateAuthAPI", // This name is used in Login.vue
                    "endpoint": "YOUR_PRIVATE_API_GATEWAY_INVOKE_URL_HERE", // Replace with actual URL after SAM deploy
                    "region": "YOUR_AWS_REGION_OF_API_GATEWAY" // e.g., "us-east-1"
                }
            ]
        }
    }
    ```

    **Option B: Configure Programmatically in `src/main.js`**
    Alternatively, you can update the configuration directly in `front/src/main.js`:
    ```javascript
    // In front/src/main.js

    // ... other imports ...
    import { Amplify, API } from 'aws-amplify'; // Ensure API is imported if not already
    import amplifyconfig from '../amplifyconfiguration.json';

    const updatedConfig = {
      ...amplifyconfig, // Spread existing config from JSON
      API: {
        endpoints: [
          {
            name: "PrivateAuthAPI",
            endpoint: "YOUR_PRIVATE_API_GATEWAY_INVOKE_URL_HERE", // Replace after SAM deploy
            region: "YOUR_AWS_REGION_OF_API_GATEWAY", // Replace
            // Amplify v6 automatically handles adding the Authorization header
            // for Cognito User Pool authorized APIs.
          }
        ]
      }
    };
    Amplify.configure(updatedConfig); // Configure Amplify with the merged settings
    // ... rest of your main.js ...
    ```

## 4. Run the Development Server

Once dependencies are installed and Amplify is configured (at least with Cognito details, API endpoint can be updated later):

*   In your terminal, ensure you are in the `front/` directory.
*   Run the development server:
    ```bash
    npm run dev
    ```
*   Vite will compile the application and provide a local URL, typically: `http://localhost:5173/`
*   Open this URL in your web browser.

## Important Considerations:

*   **Backend First:** The frontend login flow relies on the backend Lambda and API Gateway being deployed and accessible. The call to `/auth` will fail until the backend is operational and the API endpoint is correctly configured in the frontend.
*   **Google OAuth Client ID:** Ensure your Google Cloud OAuth 2.0 Client ID is correctly configured in your AWS Cognito User Pool with the right "Authorized redirect URIs". This URI must include the one Cognito uses (e.g., `https://your-cognito-domain.auth.your-region.amazoncognito.com/oauth2/idpresponse`).
*   **Callback URLs in Cognito:** In your Cognito App Client settings, ensure the "Callback URL(s)" and "Sign out URL(s)" match what you've configured in `amplifyconfiguration.json` (e.g., `http://localhost:5173/`).

By following these steps, you should be able to set up and run your Vue.js frontend application.
