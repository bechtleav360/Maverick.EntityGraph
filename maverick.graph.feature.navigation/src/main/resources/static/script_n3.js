
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
    content = ""
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

function handle_serialized_fragment(fragment) {
    let content = ""
    
    fragment.split(`\n`)
            .forEach(line => {
                converted_line = render_links(line);
                if(converted_line.trim().length > 0) {
                    content += converted_line+"\n";
                }
             });

    if(content.trim().length > 0) {
        return "<div class='fragment'>"+content+"</div>"
    } else return "";
}




var prefix = JSON.parse(document.getElementById('ns').innerText);
var contentBox = document.getElementById('rdf_box');
var currentUrl = window.location.href;


function handle_content() {
    let contentElement = document.querySelector('#content');
    let navigationElement = document.querySelector('#navigation');
    let headerElement = document.querySelector('#header');
    let detailsElement = document.querySelector('#details');

    contentElement.innerHTML = ""
    navigationElement.innerHTML = ""
    headerElement.innerHTML = ""
    detailsElement.innerHTML = ""

    const source = document.getElementById('rdf');

    const parser = new N3.Parser({ format: 'TriG*' });
    const navigationWriter = new N3.Writer({format: 'Turtle'});
    const dataWriter = new N3.Writer({format: 'Turtle'});
    const detailsWriter = new N3.Writer({format: 'application/x-turtlestar'});



    // split graph into the sections
    parser.parse(source.innerHTML,
        (error, quad) => {
            if (quad) {
                console.log(quad.graph.value)
                switch(quad.graph.value) {
                    case "urn:pwid:meg:nav":
                        navigationWriter.addQuad(quad.subject, quad.predicate, quad.object);
                        break;
                    case "urn:pwid:meg:data":
                        dataWriter.addQuad(quad.subject, quad.predicate, quad.object);
                        break;
                    case "urn:pwid:meg:details":
                        detailsWriter.addQuad(quad.subject, quad.predicate, quad.object);
                        break;
                    default:
                        console.log("No named graph: "+ quad)
                        break;
                }
            }
            else {
                navigationWriter.end((error, result) => navigationElement.innerHTML = handle_serialized_fragment(result));
                dataWriter.end((error, result) => contentElement.innerHTML = handle_serialized_fragment(result));
                detailsWriter.end((error, result) => detailsElement.innerHTML = handle_serialized_fragment(result));
            }
        },
        (prefix, iri) => {
            switch (prefix) {
                case "hydra":
                    navigationWriter.addPrefix(prefix,iri);
                    break
                case "eav":
                    detailsWriter.addPrefix(prefix,iri);
                    break
                default:
                    detailsWriter.addPrefix(prefix,iri);
                    dataWriter.addPrefix(prefix,iri);

            }
            console.log("No named graph: "+ prefix)
        }
        )
}
handle_content()

