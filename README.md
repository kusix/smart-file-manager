# Project Diagrams

## Sequence Diagrams

Our project uses Mermaid for creating sequence diagrams.

### Viewing Diagrams

You can view these diagrams using multiple methods:

1. **GitHub Native Rendering**
    - GitHub automatically renders Mermaid files

2. **Online Tools**
    - [Mermaid Live Editor](https://mermaid.live/)
    - [Mermaid.js](https://mermaid.js.org/)

3. **VS Code Extensions**
    - "Mermaid Diagram" extension
    - "Markdown Preview Mermaid Support"

### Diagram List

- `main_flow.mmd`: main process sequence

### Rendering Locally

To render Mermaid diagrams locally:

```bash
npm install -g @mermaid-js/mermaid-cli
mmdc -i input.mmd -o output.png