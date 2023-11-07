const lines = document.getElementById('rdf').innerText.split("\n");
        const prefix = JSON.parse(document.getElementById('ns').innerText);
        var pretty = ""
        lines.forEach(line => {
            line = line.replace(/[\u00A0-\u9999<>\&]/g, i => '&#' + i.charCodeAt(0) + ';')
            const nsm = line.match(/(.*)(@prefix)\s([a-z]+):\s(.*)\s/)
            if (nsm) {
                pretty += `${nsm[1]}<span class="ns">${nsm[2]} ${nsm[3]}: ${nsm[4]} .</span>`;
            } else {
                line.split(" ").forEach(token => {
                    const prm = token.match(/([a-z]+):([a-zA-Z]{1}[a-z0-9A-Z]+)/);
                    const lnm = token.match(/\&#60;((https?:\/\/{{host}}[:0-9]*)(\/[a-zA-Z0-9-_\.\/]*)\??(.*))\&#62;/);
                    if (lnm) {
                        if(lnm[3].startsWith('/api/entities') || lnm[3].startsWith('/api/s/') || lnm[3].startsWith('/nav')) {
                            pretty += `<a class="internal" rel="next" href="/nav/node?id=${encodeURIComponent(lnm[3])}&${lnm[4]}">${token}</a>`
                        }
                        else if(lnm[3].startsWith('/webjars') || lnm[3].startsWith('/v3'))  {
                            pretty += `<a class="external" target="_blank" rel="external" href="${lnm[1]}">${token}</a>`
                        }
                        else { pretty += token }
                    } else if (prm) {
                        if(prefix[prm[1]]) {
                            if(prefix[prm[1]]["external"]) {
                                pretty += `<a class="external" target="_blank" rel="external" href="${prefix[prm[1]]["url"]}${prm[2]}">${token}</a>`
                            } else {
                                pretty += `<a class="internal" rel="next" href="/nav/node?id=${token}">${token}</a>`
                            }
                        } else {
                            pretty += `<a class="internal" rel="next" href="/nav/node?id=${token}">${token}</a>`
                        }

                    } else pretty += token
                    pretty += " "
                })
            }
            pretty += "</br>"
        })


        document.querySelector('#source').innerHTML = pretty
