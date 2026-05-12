import { Main } from "./Main.js"
function importhtml(){
    document.body.innerHTML = ""
    const para = document.createElement("input")
    para.id = "IMPORTELEMENT"
    para.type = "file"
    para.addEventListener('change', function(event) {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();

        reader.onload = function(e) {
            try {
                const json = JSON.parse(e.target.result);
                const jsontxt =  JSON.stringify(json, null, 2);
                Main(jsontxt)
                console.log("Loaded JSON:", json);
            } catch (err) {
                console.error("Invalid JSON file", err);
            }
        };
        reader.readAsText(file);
    });
    document.body.appendChild(para);
}