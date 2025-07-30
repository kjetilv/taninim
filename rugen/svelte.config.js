// import adapter from '@sveltejs/adapter-auto';
import adapter from '@sveltejs/adapter-static';
import preprocess from 'svelte-preprocess';

/** @type {import('@sveltejs/kit').Config} */
const config = {
    // Consult https://kit.svelte.dev/docs/integrations#preprocessors
    // for more information about preprocessors
    preprocess: preprocess(),

    kit: {
        adapter: adapter({
            precompress: true,
            strict: true,
            fallback: "index.html"
        }),
        paths: {
            base: '/taninim-web'
        }
    }
};

export default config;
