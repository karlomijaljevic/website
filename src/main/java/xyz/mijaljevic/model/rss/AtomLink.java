package xyz.mijaljevic.model.rss;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents the <i>atom:link</i> element in the RSS XML file that the website
 * serves for RSS readers.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0
 */
@XmlRootElement(name = "link", namespace = "https://www.w3.org/2005/Atom")
public final class AtomLink
{
	private String href;
	private String rel;
	private String type;

	@XmlAttribute(name = "href")
	public String getHref()
	{
		return href;
	}

	public void setHref(String href)
	{
		this.href = href;
	}

	@XmlAttribute(name = "rel")
	public String getRel()
	{
		return rel;
	}

	public void setRel(String rel)
	{
		this.rel = rel;
	}

	@XmlAttribute(name = "type")
	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(href, rel, type);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AtomLink other = (AtomLink) obj;
		return Objects.equals(href, other.href) && Objects.equals(rel, other.rel) && Objects.equals(type, other.type);
	}

	@Override
	public String toString()
	{
		return "AtomLink [href=" + href + ", rel=" + rel + ", type=" + type + "]";
	}
}
