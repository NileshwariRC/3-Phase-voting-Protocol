package server;

import java.util.Objects;

public class Node
{

    private String hostName;
    private int port;
    private int nodeNumber;

    // test
    public Node(String hostname, int port, int nodeNumber)
    {
        this.hostName = hostname;
        this.port = port;
        this.nodeNumber = nodeNumber;
    }

    public String getHostName()
    {
        return hostName;
    }

    public int getPort()
    {
        return port;
    }

    public int getNodeNumber()
    {
        return nodeNumber;
    }

    @Override
    public String toString()
    {
        return "Node{" +
                "hostName='" + hostName + '\'' +
                ", Port=" + port +
                ", nodeNumber=" + nodeNumber +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Node that = (Node) o;
        return getPort() == that.getPort() &&
                getNodeNumber() == that.getNodeNumber() &&
                getHostName().equals(that.getHostName());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getHostName(), getPort(), getNodeNumber());
    }
}
