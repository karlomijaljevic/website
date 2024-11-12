package xyz.mijaljevic.model.rss;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents the <i>rss</i> top level (root) element in the RSS XML file that
 * the website serves for RSS readers.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0
 */
@XmlRootElement(name = "rss")
public final class Rss
{
	@XmlAttribute(name = "version")
	public static final String VERSION = "2.0";

	private Channel channel;

	@XmlElement(name = "channel")
	public Channel getChannel()
	{
		return channel;
	}

	public void setChannel(Channel channel)
	{
		this.channel = channel;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(channel);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Rss other = (Rss) obj;
		return Objects.equals(channel, other.channel);
	}

	@Override
	public String toString()
	{
		return "Rss [channel=" + channel + "]";
	}
}
