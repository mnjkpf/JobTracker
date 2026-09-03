// Status colors are now per-user hex values supplied by the API (Status.color),
// so the old static STATUS_HEX map is gone. Kept as an empty module to avoid a
// dangling import path; charts read colors straight off the status objects.
export {}
