# Mobile Performance Improvement Plan

## Problem Analysis
The user reports slow load times on mobile devices, especially when reopening an existing session. Investigation reveals a critical bottleneck in the authentication initialization flow and a lack of caching strategies suitable for mobile/unreliable networks.

### Root Causes
1.  **Blocking Network Request in Auth Initialization:**
    In `src/context/AuthContext.jsx`, the `initAuth` function waits for a network request (`/api/user`) to complete before setting `loading` to `false`.
    ```javascript
    // Current Logic
    const initAuth = async () => {
        // ... read from localStorage ...
        // ... await fetch('/api/user') ... <--- BLOCKS RENDERING
        setLoading(false); // <--- UI Renders here
    };
    ```
    This causes the user to stare at a blank screen or spinner for the duration of the network roundtrip, even if they have valid cached credentials.

2.  **Missing Service Worker:**
    The application does not have a Service Worker configured. This means:
    *   No caching of the "App Shell" (HTML, JS, CSS).
    *   Every visit requires re-downloading assets or at least checking ETags, which is slower on mobile networks.
    *   No offline capabilities.

3.  **Potential Bundle Size:**
    While code splitting (`React.lazy`) is used, the initial chunk might still be optimized further, or preloading strategies could be employed for the dashboard.

## Recommendations

### 1. Optimistic Authentication (High Impact)
Modify `src/context/AuthContext.jsx` to render immediately if valid user data is found in `localStorage`. The network verification should happen in the background.

**Proposed Logic:**
1.  Read `efina_token` and `efina_user` from `localStorage`.
2.  If found, `setUser(parsedUser)` and immediately `setLoading(false)`.
3.  Trigger the `/api/user` fetch asynchronously to update the user data/validate the token.
4.  If the background fetch fails (401), *then* log the user out and redirect to login.

### 2. Implement PWA / Service Worker (High Impact)
Add `vite-plugin-pwa` to cache static assets. This allows the browser to render the UI immediately from the disk cache while the network request (for dynamic data) is processing.

**Steps:**
*   Install `vite-plugin-pwa`.
*   Configure `vite.config.js` to generate a service worker.
*   Cache standard assets (JS, CSS, HTML, Fonts, Images).

### 3. Resource Hints
Ensure critical assets are preloaded. `index.html` already has font preconnects, but we can ensure the main entry script is treated with high priority or use HTTP/2 Server Push (via Cloudflare configuration if applicable, though Service Worker is generally better).

### 4. Optimize Backend User Endpoint
While the current query is decent, ensure the `/api/user` endpoint is strictly for *identity verification* and lightweight metadata. Avoid adding heavy aggregations (like "total balance calculation") to this endpoint in the future.

## Implementation Steps for User
1.  **Approve this plan.**
2.  I will first refactor `AuthContext.jsx` for optimistic rendering.
3.  (Optional but recommended) I will set up `vite-plugin-pwa` for reliable caching.

---

**Next Action:** Please confirm if you would like me to proceed with Refactoring `AuthContext.jsx` (Recommendation #1) immediately, or if you want the PWA setup as well.