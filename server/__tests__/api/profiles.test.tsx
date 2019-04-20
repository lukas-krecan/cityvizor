const request = require('supertest');
const app = require('../../src/routers/api');

describe('Test the profiles path', () => {
    test('It should response the GET method', (done) => {
        request(app).get('/profiles').then((response) => {
            expect(response.statusCode).toBe(200);
            done();
        });
    });
});