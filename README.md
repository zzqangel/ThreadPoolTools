# ThreadPoolTools
Offer multi type of thread pool

This project is just offered for the people who need some useful thread pool to use.

Just like sync pool, it's a pool to try to use the most of the capabilities of this computer system.

As its name, it is not an asynchronous thread pool but a synchronous one. User thread will wait for the jobs done and if the threads of this pool are all busy, those jobs will be running through the user thread.
So, no jobs will be stopped, that's the very point of this thread pool.
