const lines = document.getElementById('rdf').innerText.split("\n");
        const prefix = JSON.parse(document.getElementById('ns').innerText);
        var pretty = ""
        lines.forEach(line => {
            line = line.replace(/[\u00A0-\u9999<>\&]/g, i => '&#' + i.charCodeAt(0) + ';')
            const nsm = line.match(/(.*)(@prefix)\s([a-z]+):\s(.*)\s/)
            if (nsm) {
                pretty += `${nsm[1]}<span class="ns">${nsm[2]} ${nsm[3]} ${nsm[4]}</span>`;
            } else {
                line.split(" ").forEach(token => {
                    const prm = token.match(/([a-z]+):([a-zA-Z]{1}[a-z0-9A-Z]+)/);
                    const lnm = token.match(/\&#60;(http:\/\/localhost[:0-9]*)(\/.*)\&#62;/);
                    if (lnm) {
                        pretty += `<a class="internal" href="/nav/node?id=${encodeURIComponent(lnm[2])}">${token}</a>`
                    } else if (prm) {
                        if(prefix[prm[1]]) {
                            if(prefix[prm[1]]["external"]) {
                                pretty += `<a class="external" target="_blank" href="${prefix[prm[1]]["url"]}#${prm[2]}">${token}</a>`
                            } else {

                                pretty += `<a class="internal" href="/nav/node?id=${token}">${token}</a>`
                            }
                        } else {
                            pretty += `<a class="internal" href="/nav/node?id=${token}">${token}</a>`
                        }

                    } else pretty += token
                    pretty += " "
                })
            }
            pretty += "</br>"
        })


        document.querySelector('#source').innerHTML = pretty