import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ToastContainer } from 'react-toastify'
import 'react-toastify/dist/ReactToastify.css'

import App from '@/app/App'
import { AuthProvider } from '@/auth/AuthContext'
import '@/index.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
        <ToastContainer position="top-center" autoClose={2500} theme="colored" hideProgressBar />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
