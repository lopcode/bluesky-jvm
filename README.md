# bluesky-jvm

Tools to help you build Bluesky integrations in Java, Kotlin, and JVM systems.

Used any of the tools? I'd love to hear from more users - let me know in Discussions.

Please also give the repo a star 🌟️!

## Jetstream

*Work in progress!*

A client for the Bluesky [Jetstream](https://docs.bsky.app/blog/jetstream) service.

## Samples

*Work in progress!*

Sample projects used to validate all the tools included in this repository.

Currently connects to Jetstream (specifying zstd compression), logs ~100k events, then exits.

Run with `./run_sample.sh`, example output:
```
BUILD SUCCESSFUL in 456ms
15 actionable tasks: 15 up-to-date
[main] INFO SampleRunner - hello, bluesky
[ForkJoinPool.commonPool-worker-1] INFO Jetstream - websocket opened
[HttpClient-1-Worker-0] INFO Jetstream - 0: {"did":"did:plc:<snip>","time_us":1731721018504021,"kind":"commit","commit":{"rev":"<snip>","operation":"create","collection":"app.bsky.graph.follow","rkey":"<snip>","record":{"$type":"app.bsky.graph.follow","createdAt":"2024-11-16T01:36:59.733Z","subject":"did:plc:<snip>"},"cid":"<snip>"}}
[HttpClient-1-Worker-0] INFO Jetstream - 1: {"did":"did:plc:<snip>","time_us":1731721018505220,"kind":"commit","commit":{"rev":"<snip>","operation":"create","collection":"app.bsky.feed.like","rkey":"<snip>","record":{"$type":"app.bsky.feed.like","createdAt":"2024-11-16T01:36:58.257Z","subject":{"cid":"<snip>","uri":"at://did:plc:<snip>/app.bsky.feed.post/<snip>"}},"cid":"<snip>"}}
...
[] INFO Jetstream - exiting after 100000 messages consumed
[] INFO Jetstream - throughput: 1799.982142857143/s
```

## Contributing

I'm not currently looking for external code contributions. If you'd like to help the project:

* Use the tools, and give your feedback in [Discussions](https://github.com/lopcode/bluesky-jvm/discussions)
  * Or [file an issue](https://github.com/lopcode/bluesky-jvm/issues) if you have a problem!
* Star the repo 🌟

Thank you for being enthusiastic about the project!