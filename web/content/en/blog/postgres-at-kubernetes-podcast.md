+++
title = "Postgres at the Kubernetes Podcast"
date = 2024-06-12T16:41:00+02:00
author = ["aht"]
tags = ["postgresql", "kubernetes", "extensions"]
planet = "aht"
draft = false
readtime = "5 min"
thumbnail = "img/blog/aht-ongres-blog-postgres-kubernetes-podcast.jpg"
description = "Álvaro Hernández joins the Kubernetes Podcast episode #225 to discuss running Postgres on Kubernetes and why the operator pattern makes it work."
+++

The [Kubernetes Podcast (from Google)](https://kubernetespodcast.com/) is, probably, the most famous and reputed podcast on Kubernetes. It’s been airing for more than six years already, with 228 episodes as of today. I’ve been (and still am) an avid listener myself since 2020.

Recently I had the privilege to be invited as a guest speaker. It got published as **[Episode #225: Postgres on Kubernetes](https://kubernetespodcast.com/episode/225-pg-on-k8s/)**. What’s most important for me is that participating in this episode gave me a great opportunity, in front of a very large audience, to **advocate for Postgres among the Kubernetes Community**. Indeed, this has been the first time that Postgres has been featured on a complete episode in the Kubernetes Podcast (though there have been previous Postgres mentions in the podcast, like Gabriele Bartolini’s on [episode #222](https://kubernetespodcast.com/episode/222-kubecon-eu-2024/) or Henning Jacobs’ on [episode #38](https://kubernetespodcast.com/episode/038-kubernetes-failure-stories/)).

The episode is fully available on the [Podcast’s webpage](https://kubernetespodcast.com/episode/225-pg-on-k8s/), along with the full transcript and links mentioned during the interview. It’s also available on usual podcast platforms like Spotify.

There were a few topics from the episode that I believe are quite relevant and I’d like to highlight:

* Postgres, obviously. I discussed that running Postgres on Kubernetes is possible and safe. I even advocated that it should be your [default choice](https://thenewstack.io/kubernetes-will-revolutionize-enterprise-database-management/).
* [StackGres](https://stackgres.io/). I was asked about our open source platform for running Postgres on Kubernetes. I explained how my team came with this idea: it was not “_hey, K8s is the new kid in the block, we need to be there_” but rather “_Postgres needs a stack of components to run in production, how can we package this for any environment and architecture? With Kubernetes_”.
* [DoK](https://dok.community/). The **Data on Kubernetes Community** is a very active and large community of people interested in running data workloads on Kubernetes. This episode was also one of the first times it got explicitly mentioned in the Podcast, and in my opinion it deserves (even more) exposure. I talked about the main activities performed by the Community. Good news is that Postgres already has notable active members at DoK.
* The [(Postgres) Operator Feature Matrix](https://github.com/dokc/operator-feature-matrix), a great project by the DoK that will result in a vendor-neutral effort to help users characterize Postgres operators for Kubernetes.
* **Postgres extensions**. I couldn’t skip this topic, obviously! Lots to unpack there.
* Finally, I gave some hints into packing extensions as container images (something that was the subject of a [recent blog post I published](https://stackgres.io/blog/why-postgres-extensions-should-be-distributed-and-packaged-as-oci-images/)) and a technology I’m actively working on to allow to generate dynamically container images, so you can get the exact container Postgres container image that you want (including only the extensions that you want).

The last part of the episode is, however, my favorite. **The conversation between [Kaslin](https://twitter.com/kaslinfields) and [Abdel](https://twitter.com/boredabdel) after the interview**, reflecting on what was discussed and the learnings uncovered. I strongly recommend you to listen to the full episode!

