# Kendo MCP Setup

The Kendo MCP server for the `onestorm` tenant is already configured in this environment under
the name `kendo`, so its tools appear as `mcp__kendo__*`. Nothing needs installing.

If the tools are missing or calls fail with an auth error:

1. Run `/mcp` and check that `kendo` is listed and connected.
2. If it shows as needing authentication, re-authorize from `/mcp` (OAuth in the browser; you
   must be logged into `onestorm.kendo.dev`).
3. If `kendo` isn't listed at all, it was registered at user scope outside this repo; ask the
   user to re-add it rather than adding a different Kendo connector.

Do not fall back to the claude.ai `Kendo-*` connectors: those are other tenants, and issues
created there won't be in Bend's project.
