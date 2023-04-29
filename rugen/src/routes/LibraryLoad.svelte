<script>
    import { createEventDispatcher } from "svelte";

    const dispatch = createEventDispatcher();

    export let authorization = false;
    $: authorization = false;

    export let endpoint;

    const dispatchEvent = (event) => dispatch("receivedAlbum", event);

    const loadLibrary = async () => {
        dispatchEvent({ __clear: true });
        let response;
        try {
            let url = `${endpoint}/library.json?t=${authorization.token}`;
            response = await fetch(url);
        } catch (e) {
            return dispatchEvent({ __error: true, error: e });
        }
        if (response?.status >= 300) {
            return dispatchEvent({ __error: true, error: response.body });
        }
        if (response?.body) {
            // eslint-disable-next-line no-undef
            let reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
            let lastLine = false;
            let more = true
            do {
                const { value, done } = await reader.read();
                if (done) {
                    dispatchEvent({ __done: true });
                    more = false;
                } else {
                    let lines = value.split("\n");
                    if (lastLine) {
                        lines[0] = lastLine + lines[0];
                        lastLine = false;
                    }
                    for (const line of lines) {
                        if (line && line.length && line.length > 0) {
                            try {
                                dispatchEvent(JSON.parse(line));
                            } catch (e) {
                                if (lastLine) {
                                    throw `Already had lastline = ${lastLine}`;
                                }
                                lastLine = line;
                            }
                        }
                    }
                }
            } while (more)
        } else {
            dispatchEvent({ __error: true, error: response });
        }
    };

    $: libraryLoader = authorization && loadLibrary();
</script>
