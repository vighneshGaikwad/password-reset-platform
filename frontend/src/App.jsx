import React, { useState } from 'react';
import axios from 'axios';

function App() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage('');

    try {
      // API call to Spring Boot backend
      const response = await axios.post(
        `http://localhost:8080/api/v1/auth/request-reset?email=${encodeURIComponent(email)}`
      );
      setMessage(response.data);
    } catch (error) {
      setMessage('An error occurred while processing your request. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '420px', margin: '80px auto', fontFamily: 'sans-serif', padding: '24px', border: '1px solid #e2e8f0', borderRadius: '8px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)' }}>
      <h2 style={{ color: '#1a365d', marginBottom: '8px' }}>Password Reset</h2>
      <p style={{ color: '#4a5568', fontSize: '14px', marginBottom: '20px' }}>Enter your registered email address to receive a secure reset link.</p>
      
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '16px' }}>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 'bold', marginBottom: '6px' }}>Email Address</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="user@example.com"
            required
            style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #cbd5e0', boxSizing: 'border-box' }}
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          style={{ width: '100%', padding: '10px', backgroundColor: '#2b6cb0', color: 'white', border: 'none', borderRadius: '4px', fontWeight: 'bold', cursor: 'pointer' }}
        >
          {loading ? 'Processing...' : 'Send Reset Link'}
        </button>
      </form>

      {message && (
        <div style={{ marginTop: '20px', padding: '12px', backgroundColor: '#ebf8ff', borderLeft: '4px solid #3182ce', fontSize: '14px', color: '#2c5282' }}>
          {message}
        </div>
      )}
    </div>
  );
}

export default App;