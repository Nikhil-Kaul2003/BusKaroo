import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    // Match Spring Boot's default server.port in application.properties.
    // Override with VITE_API_PROXY_TARGET when running the existing-db profile
    // (which listens on 8082) or a separately hosted backend.
    const backendTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8081';
    return {
        plugins: [react()],
        server: {
            proxy: {
                '/api': {
                    target: backendTarget,
                    changeOrigin: true,
                },
            },
        },
        preview: {
            proxy: {
                '/api': {
                    target: backendTarget,
                    changeOrigin: true,
                },
            },
        },
    };
});
