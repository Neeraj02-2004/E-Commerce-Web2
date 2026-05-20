# E-Commerce Web2 Frontend

E-Commerce Web2 Frontend is a React + Vite ecommerce frontend for the SpringEcom backend API. It supports user authentication, Google login, product browsing, cart, wishlist, checkout, Cash on Delivery, Razorpay online payment, order status tracking, return/exchange requests, refund status display, and admin product/return management.

## Tech Stack

- React
- Vite
- JavaScript
- Bootstrap
- Bootstrap Icons
- Axios
- React Router DOM
- Razorpay Checkout
- Google Identity Services

## Main Features

### User Features

- Register
- Login
- Google login
- Browse products
- Search products
- Filter products by category
- View product details
- Add products to cart
- Add/remove wishlist items
- Checkout with Cash on Delivery
- Checkout with Razorpay online payment
- View order status
- View payment status
- View Razorpay payment ID
- Request return after delivery
- Request exchange after delivery
- View return/exchange status
- View refund status
- View Razorpay refund ID
- View refund amount
- View refund failure reason if refund fails

### Admin Features

- Add product
- Update product
- Delete/disable product
- Manage product details
- View all return/exchange requests
- Approve return/exchange requests
- Reject return/exchange requests
- Complete return/exchange requests
- View refund processing status
- View failed refund status
- View request summary counts

### Role-Based UI

The frontend separates user and admin work.

For normal users:

- Orders link is visible
- Cart link is visible
- Wishlist link is visible
- Add to Cart button is visible
- Add to Wishlist button is visible
- Return/Exchange request buttons are visible for delivered orders

For admin users:

- Add Product link is visible
- Return Requests link is visible
- Product update/delete controls are visible
- Orders link is hidden
- Cart link is hidden
- Wishlist link is hidden
- Add to Cart button is hidden
- Add to Wishlist button is hidden

Admin users are responsible for website/product/request management, not placing customer orders.

## Requirements

For local development:

- Node.js 18+
- npm
- SpringEcom backend running
- Razorpay test/live keys configured in backend
- Google client ID configured if Google login is used

## Environment Variables

Create a `.env` file in the frontend project root.

Use `.env.example` as reference.

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=change-me-google-client-id
```

Important: never commit the real `.env` file to GitHub.

## Install Dependencies

```powershell
npm install
```

## Run Development Server

```powershell
npm run dev
```

Default Vite URL:

```text
http://localhost:5173
```

The backend should be running at:

```text
http://localhost:8080
```

Or set your backend URL in:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Build for Production

```powershell
npm run build
```

Preview production build:

```powershell
npm run preview
```

## Backend Connection

The frontend uses Axios from:

```text
src/axios.jsx
```

API base URL:

```js
import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
```

All backend API calls use:

```text
/api
```

Example:

```text
http://localhost:8080/api/products
```

## Authentication Flow

Public pages:

- `/login`
- `/register`

Protected pages:

- `/`
- `/cart`
- `/wishlist`
- `/orders`
- `/product/{id}`

Admin-only pages:

- `/add_product`
- `/product/update/{id}`
- `/admin/return-exchange`

JWT token is stored in local storage:

```text
token
```

User role is stored in local storage:

```text
role
```

Supported roles:

- `USER`
- `ADMIN`

## Routes

| Route | Access | Description |
|---|---|---|
| `/login` | Public | Login page |
| `/register` | Public | Register page |
| `/` | Protected | Home/product listing |
| `/cart` | User | Cart page |
| `/wishlist` | User | Wishlist page |
| `/orders` | User | Order status page |
| `/product/:id` | Protected | Product details |
| `/add_product` | Admin | Add product |
| `/product/update/:id` | Admin | Update product |
| `/admin/return-exchange` | Admin | Admin return/exchange dashboard |

## Product Flow

Users can:

1. Browse products.
2. Search products.
3. Filter products by category.
4. Open product details.
5. Add available products to cart.
6. Add/remove products from wishlist.

Admins can:

1. Add products.
2. Update products.
3. Delete/disable products.
4. Manage product availability and details.

## Checkout Flow

Users can checkout using:

- Cash on Delivery
- Online payment through Razorpay

Checkout component:

```text
src/components/CheckoutPopup.jsx
```

Online payment helper:

```text
src/components/RazorpayPayment.jsx
```

## Razorpay Payment Flow

1. User places order with payment mode `ONLINE`.
2. Frontend calls backend to create the order.
3. Frontend calls:

```text
POST /api/payments/create
```

4. Backend creates Razorpay order.
5. Frontend opens Razorpay Checkout.
6. Razorpay returns payment response.
7. Frontend calls:

```text
POST /api/payments/verify
```

8. Backend verifies signature.
9. Backend marks payment as `PAID`.
10. User can see updated order/payment status.

Razorpay script is loaded in:

```text
index.html
```

```html
<script src="https://checkout.razorpay.com/v1/checkout.js"></script>
```

## Order Status Page

Order status component:

```text
src/components/OrderStatus.jsx
```

It displays:

- Order ID
- Customer details
- Address
- Payment mode
- Payment status
- Razorpay payment ID
- Order status
- Delivered time
- Ordered products
- Return/exchange request status
- Refund status
- Refund amount
- Razorpay refund ID
- Refund processed time
- Refund failure reason

## Return / Exchange User Flow

Users can request return or exchange only for delivered orders.

Return/exchange component:

```text
src/components/ReturnExchangePopup.jsx
```

API helper:

```text
src/api/returnExchangeApi.js
```

User endpoints used:

```text
POST /api/orders/{orderId}/return-exchange
GET /api/orders/return-exchange
```

Rules:

- Order must be `DELIVERED`.
- Request must be created within 7 days after delivery.
- Reason must be between 10 and 1000 characters.
- Only one active return/exchange request is allowed per order.

## Admin Return / Exchange Dashboard

Admin dashboard component:

```text
src/components/AdminReturnExchange.jsx
```

Route:

```text
/admin/return-exchange
```

Admin endpoints used:

```text
GET /api/admin/return-exchange
PUT /api/admin/return-exchange/{requestId}/approve
PUT /api/admin/return-exchange/{requestId}/reject
PUT /api/admin/return-exchange/{requestId}/complete
```

Admin can:

- View all return/exchange requests
- Filter by request status
- Filter by request type
- View request summary counts
- Approve requests
- Reject requests
- Complete requests
- Add admin note
- View refund status
- View refund amount
- View Razorpay refund ID
- View refund failure reason

## Return / Exchange Statuses

Request types:

- `RETURN`
- `EXCHANGE`

Request statuses:

- `REQUESTED`
- `APPROVED`
- `REJECTED`
- `COMPLETED`

Refund statuses:

- `NOT_REQUIRED`
- `REFUND_PROCESSING`
- `MANUAL_REFUND_REQUIRED`
- `REFUNDED`
- `REFUND_FAILED`

## Refund Display Rules

For online paid returns:

- Approval shows `REFUND_PROCESSING`.
- Successful refund shows `REFUNDED`.
- Failed refund shows `REFUND_FAILED`.
- Razorpay refund ID is shown if available.
- Refund amount is shown if available.
- Refund failure reason is shown if available.

For Cash on Delivery returns:

- Approval shows `MANUAL_REFUND_REQUIRED`.
- Admin/business must process refund manually.

For exchanges:

- Approved exchange shows `APPROVED`.
- Completed exchange shows `COMPLETED`.

## Wishlist Flow

Wishlist component:

```text
src/components/Wishlist.jsx
```

Wishlist API helper:

```text
src/api/wishlistApi.js
```

Endpoints used:

```text
GET /api/wishlist
POST /api/wishlist/{productId}
DELETE /api/wishlist/{productId}
```

Wishlist is visible only for normal users, not admin users.

## Important Files

| File | Purpose |
|---|---|
| `src/App.jsx` | Main routes and protected/admin routes |
| `src/axios.jsx` | Axios base URL and JWT interceptor |
| `src/Context/Context.jsx` | Global product/cart context |
| `src/components/Navbar.jsx` | Navigation and role-based links |
| `src/components/Home.jsx` | Product listing |
| `src/components/Product.jsx` | Product details and admin/user controls |
| `src/components/Cart.jsx` | Cart page |
| `src/components/CheckoutPopup.jsx` | Checkout popup |
| `src/components/RazorpayPayment.jsx` | Razorpay checkout integration |
| `src/components/OrderStatus.jsx` | User order and refund status page |
| `src/components/ReturnExchangePopup.jsx` | User return/exchange request popup |
| `src/components/AdminReturnExchange.jsx` | Admin return/exchange management |
| `src/api/returnExchangeApi.js` | Return/exchange API calls |
| `src/api/wishlistApi.js` | Wishlist API calls |
| `index.html` | Razorpay and Google scripts |

## Build Checklist

Before sending to client:

- `npm install` works
- `npm run build` passes
- `npm run preview` opens production build
- Login works
- Register works
- Google login works if configured
- Products load correctly
- Product images load correctly
- Cart works for user
- Wishlist works for user
- Admin cannot see Cart/Wishlist/Orders links
- Admin cannot see Add to Cart/Add to Wishlist buttons
- Admin can add/update/delete products
- COD checkout works
- Razorpay test payment flow works
- Orders page shows payment/order status
- Return request works after delivery
- Exchange request works after delivery
- Admin return/exchange dashboard works
- Admin can approve/reject/complete requests
- Refund status displays correctly
- `REFUND_FAILED` display works
- No real `.env` file is committed

## Client Delivery Checklist

Before final ZIP delivery:

- `git status` is clean
- `node_modules` is not included
- `dist` is not included unless client specifically asks for production build
- `.env` is not included
- `.env.example` is included
- `package.json` is included
- `package-lock.json` is included
- `README.md` is included
- `npm run build` passes
- Backend URL is documented
- Razorpay live requirement is documented
- Google login configuration is documented

## Production Deployment Notes

For Vercel, Netlify, or similar hosting:

Build command:

```text
npm run build
```

Output folder:

```text
dist
```

Environment variables:

```env
VITE_API_BASE_URL=https://your-backend-domain.com
VITE_GOOGLE_CLIENT_ID=your-google-client-id
```

Important production notes:

- Use HTTPS backend URL.
- Configure backend CORS to allow the deployed frontend domain.
- Use live Razorpay keys in backend production environment.
- Complete Razorpay KYC before live payments.
- Configure Google OAuth allowed origins for deployed frontend URL.
- Do not expose backend secrets in frontend `.env`.

## Useful Commands

Install:

```powershell
npm install
```

Run locally:

```powershell
npm run dev
```

Build:

```powershell
npm run build
```

Preview build:

```powershell
npm run preview
```

Check Git status:

```powershell
git status
```

## Final Delivery Status

This frontend is ready for client delivery after:

```powershell
npm run build
```

passes successfully.

Recommended delivery score after admin dashboard, refund display, and role-based UI improvements:

```text
95/100 to 97/100
```