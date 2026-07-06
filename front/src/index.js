import React from 'react';
import ReactDOM from 'react-dom/client';
import "@fontsource/bebas-neue";
import "@fontsource/space-mono/400.css";
import "@fontsource/space-mono/700.css";
import "@fontsource/jetbrains-mono/400.css";
import "@fontsource/jetbrains-mono/700.css";
import "./styles/tokens.css";
import "./styles/base.css";
import "./styles/effects.css";
import "./styles/shell.css";
import "./styles/modals.css";
import "./styles/actions.css";
import "./styles/tables.css";
import "./index.css";
import App from './App';
import reportWebVitals from './reportWebVitals';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);


reportWebVitals();
