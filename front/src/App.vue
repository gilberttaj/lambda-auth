<script setup>
import Login from './components/Login.vue'; // Assuming Login.vue is moved to a components subfolder
</script>

<template>
  <div id="app-container">
    <header>
      <h1>Private Lambda Authentication with Cognito</h1>
    </header>
    <main>
      <Login />
    </main>
    <footer>
      <p>&copy; 2024 Your Company</p>
    </footer>
  </div>
</template>

<style>
body {
  font-family: sans-serif;
  margin: 0;
  background-color: #f4f4f4;
  color: #333;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}

#app-container {
  background-color: #fff;
  padding: 20px 40px;
  border-radius: 8px;
  box-shadow: 0 0 15px rgba(0,0,0,0.1);
  text-align: center;
  max-width: 500px;
  width: 100%;
}

header h1 {
  color: #0056b3;
  margin-bottom: 20px;
}

main {
  margin-top: 20px;
}

footer {
  margin-top: 30px;
  font-size: 0.8em;
  color: #777;
}
</style>
