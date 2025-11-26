import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    stages: [
        { duration: '10s', target: 300 },
        { duration: '20s', target: 300 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'http_req_duration': ['p(95)<200'], // 95% of requests must complete below 200ms
        'http_req_failed': ['rate<0.01'],   // http errors should be less than 1%
    },
};

export default function () {
    const uniqueId = 300 + __VU + (__ITER * 300);
    const username = `testuser${uniqueId}`;
    const password = 'password123';

    // 1. Create a user
    const createUserPayload = JSON.stringify({
        username: username,
        password: password,
    });

    const createUserParams = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const createUserRes = http.post('http://localhost:8080/users', createUserPayload, createUserParams);

    check(createUserRes, {
        'User creation successful': (r) => r.status === 201,
    });

    if (createUserRes.status !== 201) {
        console.error(`❌ User creation failed for ${username}: ${createUserRes.status} ${createUserRes.body}`);
        return;
    }

    const userId = createUserRes.json('id');
    console.log(`✅ User creation successful for ${username}. User ID: ${userId}`);
    sleep(1);

    // 2. Login to get a token (assuming an Auth service is running on port 8081)
    const loginPayload = JSON.stringify({
        username: username,
        password: password,
    });

    const loginParams = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const loginRes = http.post('http://localhost:8081/auth/login', loginPayload, loginParams);

    check(loginRes, {
        'Login successful': (r) => r.status === 200,
    });

    if (loginRes.status !== 200) {
        console.error(`❌ Login failed for ${username}: ${loginRes.status} ${loginRes.body}`);
        return;
    }

    const accessToken = loginRes.json('accessToken');
    console.log(`✅ Login successful for ${username}`);
    sleep(1);

    // 3. Get user info
    const authHeaders = {
        headers: {
            'Authorization': `Bearer ${accessToken}`,
        },
    };

    const getMeRes = http.get('http://localhost:8080/users/me', authHeaders);

    check(getMeRes, {
        'Get my info successful': (r) => r.status === 200,
        'My info contains username': (r) => r.json('username') === username,
    });

    if (getMeRes.status === 200) {
        console.log(`✅ Get my info successful for ${username}`);
    } else {
        console.error(`❌ Get my info failed for ${username}: ${getMeRes.status} ${getMeRes.body}`);
    }
    sleep(1);

    // 4. Delete user
    const deleteRes = http.del(`http://localhost:8080/users/${userId}`, null, authHeaders);

    check(deleteRes, {
        'Delete user successful': (r) => r.status === 204,
    });

    if (deleteRes.status === 204) {
        console.log(`✅ Delete user successful for ${username}`);
    } else {
        console.error(`❌ Delete user failed for ${username}: ${deleteRes.status} ${deleteRes.body}`);
    }
    sleep(1);
}