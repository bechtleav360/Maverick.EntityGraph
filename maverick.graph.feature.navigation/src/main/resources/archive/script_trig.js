class TokenNode {
    constructor(type, value) {
        this.type = type;
        this.value = value;
        this.next = null;
        this.prev = null;
    }
}

const TokenPatterns = {
    WHITESPACE: /^\s+/,
    PREFIX: /^@prefix\s+[\w-]+:\s+<[^>]+>\s*\./i,
    BASE: /^@base\s+<[^>]+>\s*\./i,
    COMMENT: /^#.*$/,
    URI: /^<[^<>]+>/,
    BLANK_NODE: /^_:\w+/,
    BLANK_NODE_PRETTY: /^\[.*\]/,
    LITERAL: /^"[^"]*"/,
    PREFIXED_IRI: /^\w+:\w+/,
    TYPE_PREDICATE: /^a/,
    LANGTAG: /^@[a-zA-Z]+(-[a-zA-Z0-9]+)*/,
    DATATYPE: /^\^\^[^ ]+/,
    STATEMENT_LINEBREAK: /^;/,
    STATEMENT_END: /^\./,
    TRIPLE: /^<<.+>>/,
    NAMED_GRAPH_START: /^\{/,
    NAMED_GRAPH_END: /^\}/,
};
function tokenizeTurtle(turtleString) {
    const tokens = [];

    let tail = null;
    let remaining = turtleString;

    while (remaining.length > 0) {
        let matched = false;
        for (const [type, pattern] of Object.entries(TokenPatterns)) {

            const match = pattern.exec(remaining);
            if (match) {
                const newToken = new TokenNode(type, match[0].trim());
                tokens.push(newToken)

                if(type !== "WHITESPACE") {
                    if (tail) {
                        tail.next = newToken;
                        newToken.prev = tail;
                    }
                    tail = newToken;
                }

                remaining = remaining.substring(match.index + match[0].length);
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new Error(`Unexpected token at: ${remaining}`);
        }
    }
    return tokens;
}
function render_links(line) {
    let result = ""
    line.split(" ")
        // .map(token => token.replace(/[\u00A0-\u9999<>\&]/g, i => '&#' + i.charCodeAt(0) + ';'))
        .map(token => token.replace(/&#(\d+);/g, i => String.fromCharCode(i)))
        .forEach(token => {
            if(token=== "") {
                result += " "
            } else {
                var prefixedName = token.match(/^([a-z]+):([a-zA-Z]{1}[a-z0-9A-Z]+)/);
                var iri = token.match(/^<((https?:\/\/[a-zA-Z0-9-_\.]+[:0-9]*)(\/[a-zA-Z0-9-_\.#\/]*)\??(.*))>/);

                var namedGraphName = token.match(/^<(urn:pwid:meg:.+)>/); // replace tags
                var namedGraphEnd = token.match(/\}/); // add empty line after
                var namedGraphStart = token.match(/\{/); // add empty line after



                if (iri) {
                    if (iri[3].startsWith('/api/entities') || iri[3].startsWith('/api/s/')) {

                        if(iri[4]) {
                            // urls with query params are never iris, render as url
                            result += `<a class="internal" rel="next" href="${iri[3]}">${iri[1]}</a>`
                        } else {
                            // render as iri
                            result += `<a class="internal" rel="next" href="${iri[3]}">&lt;${iri[1]}&gt;</a>`
                        }
                    } else if (iri[3].startsWith('/nav')) {
                        var hrefUrl = iri[3]
                        if(iri[4]) {
                            hrefUrl = hrefUrl+"?"+iri[4]
                        }

                        result += `<a class="internal" rel="next" href="${hrefUrl}">${iri[1]}</a>`
                            // render as url
                    } else if (iri[3].startsWith('/content')) {
                        var hrefUrl = iri[3]
                        result += `<a class="content" rel="next" href="${hrefUrl}">${iri[1]}</a>`
                          // render as url
                    }
                    else if (iri[3].startsWith('/webjars') || iri[3].startsWith('/v3')) {
                        result += `<a class="external" target="_blank" rel="external" href="${iri[1]}">${iri[1]}</a>`
                    }
                    else {
                        result += "&lt;"+iri[1]+"&gt;"
                    }

                } else if (prefixedName) {
                    if (prefix[prefixedName[1]]) {
                        if (prefix[prefixedName[1]]["external"]) {
                            result += `<a class="definition" target="_blank" rel="external" href="${prefix[prefixedName[1]]["url"]}${prefixedName[2]}">${token}</a>`
                        } else {
                            result += `<a class="internal" rel="next" href="${token}">${token}</a>`
                        }
                    } else {
                        result += `<a class="internal" rel="next" href="${token}">${token}</a>`
                    }

                } else if (namedGraphName) {
                    result += "&lt;"+namedGraphName[1]+"&gt;";

                } else if (namedGraphStart) {
                    result += namedGraphStart+"\n";

                } else if (namedGraphEnd) {
                    result += "\n"+namedGraphEnd+"\n";

                } else {
                    result += token
                }
                result += " "

            }
    })
    // console.log(result)
    return result
}


function handle_prefix_fragment(fragment) { 
    let content = ""
    fragment.split(`\n`)
            .map(line => line.replace(/&#(\d+);/g, i => String.fromCharCode(i)))
            .forEach(line => {
                var matches = line.match(/(.*)(@prefix)\s([a-z]+):\s<(.*)>\s/)
                var header = `${matches[1]}<span class="ns">${matches[2]} ${matches[3]}: &lt;${matches[4]}&gt; .</span></br>`;
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
    content = ""
    
    fragment.split(`\n`)
            .forEach(line => {
                content += render_links(line)
                if(content.trim().length > 0) content += "</br>"

             });

    if(content.trim().length > 0) {
        document.querySelector('#content').innerHTML += "<div class='fragment'>"+content+"</div>"
    }
    
}




var prefix = JSON.parse(document.getElementById('ns').innerText);
var contentBox = document.getElementById('rdf_box');
var currentUrl = window.location.href;


function handle_content() {
    document.querySelector('#content').innerHTML = ""
    document.querySelector('#navigation').innerHTML = ""
    document.querySelector('#header').innerHTML = ""

    const content = document.getElementById('rdf')
    let tokens = tokenizeTurtle(content.innerHTML)

    tokens.forEach(token => {
        if(token.type === "PREFIX") {
            handle_prefix_fragment(token.value)
        }

        if(token.type === "NAMED_GRAPH_START") {
            named_graph_iri = token.prev
            console.log(named_graph_iri)
        }
    })

    var fragments = content.innerHTML.split("\n\n");
    console.log(fragments.length)

    fragments.forEach(fragment => {
        fragment = fragment.trim()
        token = fragment.split('\n')[0].split(' ')
        if(token[0] && token[0].match(/@prefix/)) {
            return handle_prefix_fragment(fragment)
        }
        if(token[2] && token[2].match(/.*hydra.*/)) {
            return handle_hydra_fragment(fragment)
        }

        return handle_content_fragment(fragment)
    })

    var links = document.querySelectorAll(".internal");

    links.forEach(function(link) {
        link.addEventListener("click", function(event) {
            event.preventDefault();

            var clickedUrl = this.href;
            var requestUrl = clickedUrl.replace("/api", "/nav");

            // FIXME: append current session url

            window.location.href=requestUrl

            return;
        });
    });
}
handle_content()

