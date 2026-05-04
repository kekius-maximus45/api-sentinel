# MCP And AI Notes

Spring AI MCP is enabled with the WebMVC server starter and Streamable HTTP transport:

```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE
        name: api-sentinel-mcp
```

The MCP tools mirror service-layer read operations and require an API key. `create_alert_rule` is intentionally gated with an explicit `confirmed` boolean so MCP clients cannot change alert configuration by accident.

The REST incident assistant uses a provider abstraction:

- `mock`: deterministic local response for demos and tests.
- `openai`: OpenAI-compatible `/chat/completions` endpoint configured by base URL, key, and model.

Every assistant call writes an audit log with user, organization, prompt summary, tools invoked, result status, and timestamp.
