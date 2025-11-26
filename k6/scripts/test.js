import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 300,
    duration: '10s',
};

export default function () {
    const username = `testuser${__VU}`;
    const credentials = {
        username: username,
        password: 'password',
    };

    const loginRes = http.post('http://localhost:8080/auth/login', JSON.stringify(credentials), {
        headers: { 'Content-Type': 'application/json' },
    });

    check(loginRes, { 'login successful': (r) => r.status === 200 });

    if (loginRes.status !== 200) {
        console.error(`❌ Login failed for ${username}: ${loginRes.status} ${loginRes.body}`);
        return;
    }

    let accessToken = loginRes.json('accessToken');
    const refreshToken = loginRes.json('refreshToken');
    console.log(`✅ Login successful for ${username}`);

    sleep(1);

    const jar = http.cookieJar();
    jar.set('http://localhost:8080', 'refreshToken', refreshToken);

    const refreshRes = http.post('http://localhost:8080/auth/refresh', null, {
        headers: { 'Content-Type': 'application/json' },
        cookies: jar.cookiesForURL('http://localhost:8080'),
    });

    check(refreshRes, { 'token refresh successful': (r) => r.status === 200 });

    if (refreshRes.status === 200) {
        accessToken = refreshRes.json('accessToken');
        console.log(`🔄 Access token refreshed for ${username}`);
    } else {
        console.error(`❌ Failed to refresh token for ${username}: ${refreshRes.status} ${refreshRes.body}`);
    }

    sleep(1);

    const logoutRes = http.post('http://localhost:8080/auth/logout', null, {
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json',
        },
        cookies: jar.cookiesForURL('http://localhost:8080'),
    });

    check(logoutRes, { 'logout successful': (r) => r.status === 200 });

    if (logoutRes.status === 200) {
        console.log(`👋 Logout successful for ${username}`);
    } else {
        console.error(`❌ Logout failed for ${username}: ${logoutRes.status} ${logoutRes.body}`);
    }

    sleep(1);
}