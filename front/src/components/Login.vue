<template>
    <div class="login-container">
      <div v-if="!authStore.isAuthenticated && !authStore.isLoading">
        <h2>Login with Google</h2>
        <button @click="signInWithGoogle" :disabled="authStore.isLoading">
          <span v-if="authStore.isLoading">Loading...</span>
          <span v-else>Sign In with Google</span>
        </button>
      </div>
      <div v-if="authStore.isAuthenticated">
        <h3>Welcome, {{ authStore.user?.attributes?.email }}!</h3>
        <p>Your ID Token (first 30 chars): {{ authStore.idToken?.substring(0, 30) }}...</p>
        <p>Auth API Response: {{ authStore.authApiResponse }}</p>
        <button @click="signOut" :disabled="authStore.isLoading">Sign Out</button>
      </div>
      <div v-if="authStore.error">
        <p class="error-message">Error: {{ authStore.error }}</p>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted, reactive } from 'vue';
  import { Auth, API } from 'aws-amplify';
  import { Hub } from 'aws-amplify/utils'; // Updated import for Hub
  
  // Simple reactive store for auth state (in a real app, consider Pinia or Vuex)
  const authStore = reactive({
    user: null,
    idToken: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,
    authApiResponse: null,
  });
  
  const signInWithGoogle = async () => {
    authStore.isLoading = true;
    authStore.error = null;
    try {
      // This will redirect to the Cognito Hosted UI for Google login
      await Auth.federatedSignIn({ provider: 'Google' });
    } catch (error) {
      console.error('Error signing in with Google:', error);
      authStore.error = error.message || 'Failed to initiate Google sign-in.';
      authStore.isLoading = false;
    }
  };
  
  const signOut = async () => {
    authStore.isLoading = true;
    authStore.error = null;
    try {
      await Auth.signOut();
      // Hub listener will clear user state
    } catch (error) {
      console.error('Error signing out: ', error);
      authStore.error = error.message || 'Failed to sign out.';
      authStore.isLoading = false; // Ensure loading is reset even on error
    }
  };
  
  const callAuthLambda = async (token) => {
    authStore.isLoading = true;
    authStore.authApiResponse = null;
    authStore.error = null;
    try {
      const apiName = 'PrivateAuthAPI'; // Matches the name in Amplify.configure
      const path = '/auth'; // Your Lambda's path
      const myInit = {
        body: { idToken: token }, // Send ID token in the body
        headers: {
          'Content-Type': 'application/json',
        },
      };
      const response = await API.post(apiName, path, myInit);
      console.log('Auth Lambda response:', response);
      authStore.authApiResponse = response; // Or handle specific data from response
      // Potentially update isAuthenticated based on this response if needed,
      // though Cognito session already implies some level of auth.
    } catch (error) {
      console.error('Error calling auth Lambda:', error);
      authStore.error = error.response?.data?.error || error.message || 'Failed to call auth API.';
      authStore.authApiResponse = { error: authStore.error };
       // If the lambda returns 403, you might want to sign the user out or show a specific message
      if (error.response?.status === 403) {
          authStore.error = "Access Denied: Your email domain is not authorized.";
          // Optionally sign out if domain is not authorized
          // await signOut(); 
      }
    } finally {
      authStore.isLoading = false;
    }
  };
  
  // Hub listener for auth events (sign-in, sign-out)
  Hub.listen('auth', ({ payload }) => {
    const { event, data } = payload;
    switch (event) {
      case 'signInWithRedirect': // This event fires when redirect from Cognito Hosted UI starts
        authStore.isLoading = true;
        break;
      case 'signIn': // This event fires after successful sign-in (if not using redirect for the final step)
      case 'cognitoHostedUI': // This event fires after successful sign-in via Hosted UI
        console.log('Hub: signIn or cognitoHostedUI event', data);
        authStore.user = data; // data is the CognitoUser object
        authStore.isAuthenticated = true;
        authStore.isLoading = false;
        authStore.error = null;
        // Get session to retrieve ID token
        Auth.currentSession()
          .then(session => {
            const idToken = session.getIdToken().getJwtToken();
            authStore.idToken = idToken;
            console.log('ID Token:', idToken);
            // Call your custom Lambda for domain validation
            callAuthLambda(idToken);
          })
          .catch(err => {
            console.error('Error getting current session:', err);
            authStore.error = 'Failed to get user session.';
            authStore.isAuthenticated = false;
            authStore.isLoading = false;
          });
        break;
      case 'signOut':
        console.log('Hub: signOut event');
        authStore.user = null;
        authStore.idToken = null;
        authStore.isAuthenticated = false;
        authStore.isLoading = false;
        authStore.error = null;
        authStore.authApiResponse = null;
        break;
      case 'signIn_failure':
      case 'cognitoHostedUI_failure':
        console.error('Hub: sign in failure event', data);
        authStore.error = data?.message || 'Sign-in failed.';
        authStore.isAuthenticated = false;
        authStore.isLoading = false;
        break;
      default:
        // console.log('Hub: other auth event', event, data);
        break;
    }
  });
  
  // Check current authenticated user on component mount
  onMounted(async () => {
    authStore.isLoading = true;
    try {
      const cognitoUser = await Auth.currentAuthenticatedUser();
      authStore.user = cognitoUser; // This is the CognitoUser object
      authStore.isAuthenticated = true;
  
      const session = await Auth.currentSession();
      const idToken = session.getIdToken().getJwtToken();
      authStore.idToken = idToken;
      console.log('Current ID Token on mount:', idToken);
      await callAuthLambda(idToken); // Call your lambda with the token
  
    } catch (error) {
      console.log('No authenticated user found on mount or error fetching session:', error);
      authStore.user = null;
      authStore.idToken = null;
      authStore.isAuthenticated = false;
      // Don't set global error for this case, it's normal if not logged in
    } finally {
      authStore.isLoading = false;
    }
  });
  </script>
  
  <style scoped>
  .login-container {
    max-width: 400px;
    margin: 50px auto;
    padding: 20px;
    border: 1px solid #ccc;
    border-radius: 8px;
    text-align: center;
  }
  button {
    padding: 10px 20px;
    font-size: 16px;
    cursor: pointer;
    background-color: #007bff;
    color: white;
    border: none;
    border-radius: 4px;
    margin-top: 10px;
  }
  button:disabled {
    background-color: #ccc;
  }
  .error-message {
    color: red;
    margin-top: 15px;
  }
  </style>
