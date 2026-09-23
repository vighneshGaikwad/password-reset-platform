import React, { useState, useEffect } from 'react';
import axios from 'axios';

function App() {
  const [activeTab, setActiveTab] = useState('user'); // 'user' or 'admin'
  
  // User Form State
  const [email, setEmail] = useState('');
  const [userMsg, setUserMsg] = useState('');

  // Admin State
  const [summary, setSummary] = useState({});
  const [requests, setRequests] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  
  // New User State
  const [newUserEmail, setNewUserEmail] = useState('');
  const [newUsername, setNewUsername] = useState('');

  useEffect(() => {
    if (activeTab === 'admin') {
      fetchAdminData();
    }
  }, [activeTab]);

  const fetchAdminData = async () => {
    try {
      const summaryRes = await axios.get('http://localhost:8080/api/v1/admin/dashboard/summary');
      setSummary(summaryRes.data);
      
      const requestsRes = await axios.get(`http://localhost:8080/api/v1/admin/requests?search=${searchTerm}`);
      setRequests(requestsRes.data);
    } catch (err) {
      console.error('Error fetching admin data:', err);
    }
  };

  const handleResetRequest = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post(`http://localhost:8080/api/v1/auth/request-reset?email=${encodeURIComponent(email)}`);
      setUserMsg(res.data);
    } catch (err) {
      setUserMsg('Failed to process request.');
    }
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    try {
      await axios.post('http://localhost:8080/api/v1/admin/users', {
        email: newUserEmail,
        username: newUsername,
        status: 'ACTIVE'
      });
      alert('User created successfully!');
      setNewUserEmail('');
      setNewUsername('');
      fetchAdminData();
    } catch (err) {
      alert('Error creating user.');
    }
  };

  const handleStatusUpdate = async (id, newStatus) => {
    try {
      await axios.put(`http://localhost:8080/api/v1/admin/requests/${id}/status?status=${newStatus}`);
      fetchAdminData();
    } catch (err) {
      alert('Failed to update status.');
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: 'sans-serif', maxWidth: '1000px', margin: '0 auto' }}>
      {/* Navigation Header */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '24px', borderBottom: '2px solid #e2e8f0', pb: '12px' }}>
        <button 
          onClick={() => setActiveTab('user')}
          style={{ padding: '10px 20px', fontWeight: 'bold', cursor: 'pointer', background: activeTab === 'user' ? '#2b6cb0' : '#e2e8f0', color: activeTab === 'user' ? '#fff' : '#000', border: 'none', borderRadius: '4px' }}>
          User Reset Portal
        </button>
        <button 
          onClick={() => setActiveTab('admin')}
          style={{ padding: '10px 20px', fontWeight: 'bold', cursor: 'pointer', background: activeTab === 'admin' ? '#2b6cb0' : '#e2e8f0', color: activeTab === 'admin' ? '#fff' : '#000', border: 'none', borderRadius: '4px' }}>
          Admin Dashboard & Workflow
        </button>
      </div>

      {/* USER VIEW */}
      {activeTab === 'user' && (
        <div style={{ maxWidth: '400px', margin: '40px auto', border: '1px solid #ccc', padding: '20px', borderRadius: '8px' }}>
          <h2>Request Password Reset</h2>
          <form onSubmit={handleResetRequest}>
            <input 
              type="email" 
              placeholder="Enter user email..." 
              value={email} 
              onChange={e => setEmail(e.target.value)} 
              required 
              style={{ width: '100%', padding: '8px', marginBottom: '12px', boxSizing: 'border-box' }}
            />
            <button type="submit" style={{ width: '100%', padding: '10px', background: '#2b6cb0', color: '#fff', border: 'none', borderRadius: '4px' }}>
              Submit Request
            </button>
          </form>
          {userMsg && <p style={{ marginTop: '12px', color: '#2b6cb0' }}>{userMsg}</p>}
        </div>
      )}

      {/* ADMIN VIEW */}
      {activeTab === 'admin' && (
        <div>
          {/* Dashboard Cards */}
          <h3>Summary Dashboard</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginBottom: '24px' }}>
            <div style={{ background: '#ebf8ff', padding: '16px', borderRadius: '6px', textAlign: 'center' }}>
              <h4>Total Users</h4>
              <h2>{summary.totalUsers || 0}</h2>
            </div>
            <div style={{ background: '#feebc8', padding: '16px', borderRadius: '6px', textAlign: 'center' }}>
              <h4>Pending Requests</h4>
              <h2>{summary.pendingRequests || 0}</h2>
            </div>
            <div style={{ background: '#c6f6d5', padding: '16px', borderRadius: '6px', textAlign: 'center' }}>
              <h4>Completed</h4>
              <h2>{summary.completedRequests || 0}</h2>
            </div>
            <div style={{ background: '#fed7d7', padding: '16px', borderRadius: '6px', textAlign: 'center' }}>
              <h4>Rejected</h4>
              <h2>{summary.rejectedRequests || 0}</h2>
            </div>
          </div>

          {/* Create User Form */}
          <div style={{ background: '#f7fafc', padding: '16px', borderRadius: '6px', marginBottom: '24px' }}>
            <h4>Create New User Record</h4>
            <form onSubmit={handleCreateUser} style={{ display: 'flex', gap: '12px' }}>
              <input type="text" placeholder="Username" value={newUsername} onChange={e => setNewUsername(e.target.value)} required style={{ padding: '8px' }} />
              <input type="email" placeholder="Email" value={newUserEmail} onChange={e => setNewUserEmail(e.target.value)} required style={{ padding: '8px' }} />
              <button type="submit" style={{ background: '#38a169', color: '#fff', border: 'none', padding: '8px 16px', borderRadius: '4px' }}>Add User</button>
            </form>
          </div>

          {/* Search and Table */}
          <h4>Manage Reset Requests & Workflow</h4>
          <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
            <input 
              type="text" 
              placeholder="Search by Email..." 
              value={searchTerm} 
              onChange={e => setSearchTerm(e.target.value)} 
              style={{ padding: '8px', width: '300px' }}
            />
            <button onClick={fetchAdminData} style={{ padding: '8px 16px', background: '#4a5568', color: '#fff', border: 'none', borderRadius: '4px' }}>Search</button>
          </div>

          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: '#edf2f7' }}>
                <th style={{ padding: '10px', border: '1px solid #cbd5e0' }}>ID</th>
                <th style={{ padding: '10px', border: '1px solid #cbd5e0' }}>Email</th>
                <th style={{ padding: '10px', border: '1px solid #cbd5e0' }}>Status</th>
                <th style={{ padding: '10px', border: '1px solid #cbd5e0' }}>Actions Workflow</th>
              </tr>
            </thead>
            <tbody>
              {requests.map(req => (
                <tr key={req.id}>
                  <td style={{ padding: '10px', border: '1px solid #cbd5e0' }}>{req.id}</td>
                  <td style={{ padding: '10px', border: '1px solid #cbd5e0' }}>{req.userEmail}</td>
                  <td style={{ padding: '10px', border: '1px solid #cbd5e0', fontWeight: 'bold' }}>{req.status}</td>
                  <td style={{ padding: '10px', border: '1px solid #cbd5e0', display: 'flex', gap: '8px' }}>
                    <button onClick={() => handleStatusUpdate(req.id, 'APPROVED')} style={{ background: '#3182ce', color: '#fff', border: 'none', padding: '4px 8px', borderRadius: '4px' }}>Approve</button>
                    <button onClick={() => handleStatusUpdate(req.id, 'COMPLETED')} style={{ background: '#38a169', color: '#fff', border: 'none', padding: '4px 8px', borderRadius: '4px' }}>Complete</button>
                    <button onClick={() => handleStatusUpdate(req.id, 'REJECTED')} style={{ background: '#e53e3e', color: '#fff', border: 'none', padding: '4px 8px', borderRadius: '4px' }}>Reject</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default App;