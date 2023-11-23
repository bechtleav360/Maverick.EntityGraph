
function render_links(line) {
    result = ""
    line.split(" ")
        .map(line => line.replace(/[\u00A0-\u9999<>\&]/g, i => '&#' + i.charCodeAt(0) + ';'))
        .forEach(token => {
            const prm = token.match(/([a-z]+):([a-zA-Z]{1}[a-z0-9A-Z]+)/);
            const lnm = token.match(/\&#60;((https?:\/\/localhost[:0-9]*)(\/[a-zA-Z0-9-_\.\/]*)\??(.*))\&#62;/);
            if (lnm) {
                if (lnm[3].startsWith('/api/entities') || lnm[3].startsWith('/api/s/') || lnm[3].startsWith('/nav')) {
                    result += `<a class="internal" rel="next" href="/nav/node?id=${encodeURIComponent(lnm[3])}&${lnm[4]}">${token}</a>`
                }
                else if (lnm[3].startsWith('/webjars') || lnm[3].startsWith('/v3')) {
                    result += `<a class="external" target="_blank" rel="external" href="${lnm[1]}">${token}</a>`
                }
                else { result += token }
            } else if (prm) {
                if (prefix[prm[1]]) {
                    if (prefix[prm[1]]["external"]) {
                        result += `<a class="definition" target="_blank" rel="external" href="${prefix[prm[1]]["url"]}${prm[2]}">${token}</a>`
                    } else {
                        result += `<a class="internal" rel="next" href="/nav/node?id=${token}">${token}</a>`
                    }
                } else {
                    result += `<a class="internal" rel="next" href="/nav/node?id=${token}">${token}</a>`
                }

            } else result += token
            result += " "
    })
    return result
}


function handle_prefix_fragment(fragment) { 
    content = ""
    fragment.split(`\n`)
            .map(line => line.replace(/[\u00A0-\u9999<>\&]/g, i => '&#' + i.charCodeAt(0) + ';'))
            .forEach(line => {
                const matches = line.match(/(.*)(@prefix)\s([a-z]+):\s(.*)\s/)
                const header = `${matches[1]}<span class="ns">${matches[2]} ${matches[3]}: ${matches[4]} .</span></br>`;
                content += header
             });  
    document.querySelector('#header').innerHTML +=  "<div class='fragment'>"+content+"</div>"
}

function handle_hydra_fragment(fragment) {
    content = ""
    fragment.split(`\n`).forEach(line => {
        content += render_links(line)+"</br>"
    }); 
    
    document.querySelector('#navigation').innerHTML += "<div class='fragment'>"+content+"</div>"
}

function handle_content_fragment(fragment) {
    document.querySelector('#content').innerHTML += fragment
}



const prefix = JSON.parse(document.getElementById('ns').innerText);
const fragments = document.getElementById('rdf').innerText.split("\n\n");
console.log(fragments.length)

fragments.forEach(fragment => {
    fragment = fragment.trim()
    token = fragment.split('\n')[0].split(' ')
    if(token[0].match(/@prefix/)) {
        return handle_prefix_fragment(fragment)
    }
    
    if(token[2].match(/.*hydra.*/)) {
        return handle_hydra_fragment(fragment)
    }

    return handle_content_fragment(fragment)
   

})

