const { useState, useEffect } = React;

// Main App Component
function App() {
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ text: '', type: '' });

    // Fetch transaction history
    const fetchTransactions = async () => {
        try {
            const response = await fetch('/api/payments/history');
            if (response.ok) {
                const data = await response.json();
                setTransactions(data);
            }
        } catch (error) {
            console.error('Error fetching transactions:', error);
        }
    };

    // Auto-refresh every 5 seconds
    useEffect(() => {
        fetchTransactions();
        const interval = setInterval(fetchTransactions, 5000);
        return () => clearInterval(interval);
    }, []);

    // Show message with auto-dismiss
    const showMessage = (text, type) => {
        setMessage({ text, type });
        setTimeout(() => setMessage({ text: '', type: '' }), 5000);
    };

    return (
        <div className="app-container">
            <header className="app-header">
                <h1>💳 Payment Gateway Demo</h1>
                <p>Modern Payment Processing System</p>
            </header>

            {message.text && (
                <div className={`message ${message.type}`}>
                    {message.text}
                </div>
            )}

            <div className="content-grid">
                <PaymentForm 
                    onSuccess={() => {
                        fetchTransactions();
                        showMessage('Payment authorized successfully!', 'success');
                    }}
                    onError={(error) => showMessage(error, 'error')}
                    setLoading={setLoading}
                />
                <TransactionHistory 
                    transactions={transactions}
                    onRefresh={fetchTransactions}
                    onSuccess={(msg) => showMessage(msg, 'success')}
                    onError={(error) => showMessage(error, 'error')}
                    loading={loading}
                    setLoading={setLoading}
                />
            </div>
        </div>
    );
}

// Payment Form Component
function PaymentForm({ onSuccess, onError, setLoading }) {
    const [formData, setFormData] = useState({
        cardNumber: '',
        expiryDate: '',
        cvv: '',
        amount: ''
    });
    const [errors, setErrors] = useState({});

    // Format card number with spaces
    const formatCardNumber = (value) => {
        const cleaned = value.replace(/\s/g, '');
        const chunks = cleaned.match(/.{1,4}/g);
        return chunks ? chunks.join(' ') : cleaned;
    };

    // Format expiry date as MM/YY
    const formatExpiryDate = (value) => {
        const cleaned = value.replace(/\D/g, '');
        if (cleaned.length >= 2) {
            return cleaned.slice(0, 2) + '/' + cleaned.slice(2, 4);
        }
        return cleaned;
    };

    // Handle input changes
    const handleChange = (e) => {
        const { name, value } = e.target;
        let formattedValue = value;

        if (name === 'cardNumber') {
            formattedValue = formatCardNumber(value.replace(/\D/g, '').slice(0, 16));
        } else if (name === 'expiryDate') {
            formattedValue = formatExpiryDate(value.slice(0, 5));
        } else if (name === 'cvv') {
            formattedValue = value.replace(/\D/g, '').slice(0, 4);
        } else if (name === 'amount') {
            formattedValue = value.replace(/[^\d.]/g, '');
        }

        setFormData(prev => ({ ...prev, [name]: formattedValue }));
        setErrors(prev => ({ ...prev, [name]: '' }));
    };

    // Validate form
    const validateForm = () => {
        const newErrors = {};
        const cardNumberClean = formData.cardNumber.replace(/\s/g, '');

        if (!cardNumberClean || cardNumberClean.length < 13 || cardNumberClean.length > 16) {
            newErrors.cardNumber = 'Card number must be 13-16 digits';
        }

        if (!formData.expiryDate || !/^\d{2}\/\d{2}$/.test(formData.expiryDate)) {
            newErrors.expiryDate = 'Expiry date must be MM/YY format';
        } else {
            const [month, year] = formData.expiryDate.split('/');
            if (parseInt(month) < 1 || parseInt(month) > 12) {
                newErrors.expiryDate = 'Invalid month';
            }
        }

        if (!formData.cvv || formData.cvv.length < 3 || formData.cvv.length > 4) {
            newErrors.cvv = 'CVV must be 3-4 digits';
        }

        if (!formData.amount || parseFloat(formData.amount) <= 0) {
            newErrors.amount = 'Amount must be greater than 0';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    // Submit payment
    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!validateForm()) {
            return;
        }

        setLoading(true);
        try {
            const response = await fetch('/api/payments/authorize', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    cardNumber: formData.cardNumber.replace(/\s/g, ''),
                    expiryDate: formData.expiryDate,
                    cvv: formData.cvv,
                    amount: parseFloat(formData.amount)
                })
            });

            const data = await response.json();

            if (response.ok && data.status === 'AUTHORIZED') {
                setFormData({ cardNumber: '', expiryDate: '', cvv: '', amount: '' });
                onSuccess();
            } else {
                onError(data.message || 'Payment authorization failed');
            }
        } catch (error) {
            onError('Network error. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card payment-form-card">
            <h2>💰 New Payment</h2>
            
            <div className="test-cards">
                <h3>Test Card Numbers:</h3>
                <div className="test-card-list">
                    <div className="test-card-item">
                        <span className="card-type">Visa:</span>
                        <code>4263 9700 0000 5262</code>
                    </div>
                    <div className="test-card-item">
                        <span className="card-type">MasterCard:</span>
                        <code>5425 2300 0000 4415</code>
                    </div>
                    <div className="test-card-item">
                        <span className="card-type">Amex:</span>
                        <code>3741 010000 00608</code>
                    </div>
                </div>
            </div>

            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="cardNumber">Card Number</label>
                    <input
                        type="text"
                        id="cardNumber"
                        name="cardNumber"
                        value={formData.cardNumber}
                        onChange={handleChange}
                        placeholder="XXXX XXXX XXXX XXXX"
                        className={errors.cardNumber ? 'error' : ''}
                    />
                    {errors.cardNumber && <span className="error-text">{errors.cardNumber}</span>}
                </div>

                <div className="form-row">
                    <div className="form-group">
                        <label htmlFor="expiryDate">Expiry Date</label>
                        <input
                            type="text"
                            id="expiryDate"
                            name="expiryDate"
                            value={formData.expiryDate}
                            onChange={handleChange}
                            placeholder="MM/YY"
                            className={errors.expiryDate ? 'error' : ''}
                        />
                        {errors.expiryDate && <span className="error-text">{errors.expiryDate}</span>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="cvv">CVV</label>
                        <input
                            type="text"
                            id="cvv"
                            name="cvv"
                            value={formData.cvv}
                            onChange={handleChange}
                            placeholder="123"
                            className={errors.cvv ? 'error' : ''}
                        />
                        {errors.cvv && <span className="error-text">{errors.cvv}</span>}
                    </div>
                </div>

                <div className="form-group">
                    <label htmlFor="amount">Amount ($)</label>
                    <input
                        type="text"
                        id="amount"
                        name="amount"
                        value={formData.amount}
                        onChange={handleChange}
                        placeholder="0.00"
                        className={errors.amount ? 'error' : ''}
                    />
                    {errors.amount && <span className="error-text">{errors.amount}</span>}
                </div>

                <button type="submit" className="btn btn-primary">
                    Authorize Payment
                </button>
            </form>
        </div>
    );
}

// Transaction History Component
function TransactionHistory({ transactions, onRefresh, onSuccess, onError, loading, setLoading }) {
    const [actionLoading, setActionLoading] = useState({});

    // Handle capture
    const handleCapture = async (transactionId) => {
        setActionLoading(prev => ({ ...prev, [transactionId]: 'capture' }));
        setLoading(true);
        try {
            const response = await fetch('/api/payments/capture', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ transactionId })
            });

            const data = await response.json();

            if (response.ok && data.status === 'CAPTURED') {
                onSuccess('Payment captured successfully!');
                onRefresh();
            } else {
                onError(data.message || 'Capture failed');
            }
        } catch (error) {
            onError('Network error. Please try again.');
        } finally {
            setActionLoading(prev => ({ ...prev, [transactionId]: null }));
            setLoading(false);
        }
    };

    // Handle refund
    const handleRefund = async (transactionId) => {
        setActionLoading(prev => ({ ...prev, [transactionId]: 'refund' }));
        setLoading(true);
        try {
            const response = await fetch('/api/payments/refund', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ transactionId })
            });

            const data = await response.json();

            if (response.ok && data.status === 'REFUNDED') {
                onSuccess('Payment refunded successfully!');
                onRefresh();
            } else {
                onError(data.message || 'Refund failed');
            }
        } catch (error) {
            onError('Network error. Please try again.');
        } finally {
            setActionLoading(prev => ({ ...prev, [transactionId]: null }));
            setLoading(false);
        }
    };

    // Format date
    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    // Mask card number
    const maskCardNumber = (cardNumber) => {
        if (!cardNumber) return '';
        return '**** **** **** ' + cardNumber.slice(-4);
    };

    return (
        <div className="card transaction-history-card">
            <div className="card-header">
                <h2>📊 Transaction History</h2>
                <button onClick={onRefresh} className="btn btn-secondary" disabled={loading}>
                    🔄 Refresh
                </button>
            </div>

            {transactions.length === 0 ? (
                <div className="empty-state">
                    <p>No transactions yet</p>
                    <p className="empty-state-subtitle">Submit a payment to get started</p>
                </div>
            ) : (
                <div className="table-container">
                    <table className="transaction-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Card</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {transactions.map(transaction => (
                                <tr key={transaction.id}>
                                    <td className="transaction-id">{transaction.id.slice(0, 8)}...</td>
                                    <td>{maskCardNumber(transaction.cardNumber)}</td>
                                    <td className="amount">${transaction.amount.toFixed(2)}</td>
                                    <td>
                                        <span className={`status-badge status-${transaction.status.toLowerCase()}`}>
                                            {transaction.status}
                                        </span>
                                    </td>
                                    <td className="date">{formatDate(transaction.timestamp)}</td>
                                    <td className="actions">
                                        {transaction.status === 'AUTHORIZED' && (
                                            <button
                                                onClick={() => handleCapture(transaction.id)}
                                                className="btn btn-small btn-success"
                                                disabled={actionLoading[transaction.id]}
                                            >
                                                {actionLoading[transaction.id] === 'capture' ? '...' : 'Capture'}
                                            </button>
                                        )}
                                        {transaction.status === 'CAPTURED' && (
                                            <button
                                                onClick={() => handleRefund(transaction.id)}
                                                className="btn btn-small btn-warning"
                                                disabled={actionLoading[transaction.id]}
                                            >
                                                {actionLoading[transaction.id] === 'refund' ? '...' : 'Refund'}
                                            </button>
                                        )}
                                        {(transaction.status === 'DECLINED' || transaction.status === 'REFUNDED') && (
                                            <span className="no-action">-</span>
                                        )}
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

// Render the app
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);

// Made with Bob
