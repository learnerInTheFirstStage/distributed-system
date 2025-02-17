###
For MultithreadedClient, SingleThreadClient and InstrumentedClient, you could directly
run the main method inside it recursively.

But there is one part you need to modify, which is the baseUrl.
You can change the baseUrl in the main method using your ec2 instance
public IP address.