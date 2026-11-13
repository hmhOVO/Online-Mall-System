import {fileURLToPath, URL} from 'node:url'

import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from "unplugin-vue-components/resolvers";

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        vue(),
        AutoImport({
            resolvers: [ElementPlusResolver()],
        }),
        components({
            resolvers: [
                ElementPlusResolver({importStyle: "sass"})
            ],
        }),
    ],
    css: {
        preprocessorOptions: {
            scss: {
                additionalData: `@use "@/assets/css/index.scss" as *;`,
            },
        },
    },
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        },
    },
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:9999',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, '')
            },
            '/upload': {
                target: 'http://localhost:9999',
                changeOrigin: true
            },
            '/ws': {
                target: 'ws://localhost:9999',
                ws: true,
                changeOrigin: true
            }
        }
    }
})
