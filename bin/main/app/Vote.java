//
// Copyright (c) ZeroC, Inc. All rights reserved.
//
//
// Ice version 3.7.10
//
// <auto-generated>
//
// Generated from file `Service.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

package app;

public class Vote implements java.lang.Cloneable,
                             java.io.Serializable
{
    public String id;

    public String vote;

    public Vote()
    {
        this.id = "";
        this.vote = "";
    }

    public Vote(String id, String vote)
    {
        this.id = id;
        this.vote = vote;
    }

    public boolean equals(java.lang.Object rhs)
    {
        if(this == rhs)
        {
            return true;
        }
        Vote r = null;
        if(rhs instanceof Vote)
        {
            r = (Vote)rhs;
        }

        if(r != null)
        {
            if(this.id != r.id)
            {
                if(this.id == null || r.id == null || !this.id.equals(r.id))
                {
                    return false;
                }
            }
            if(this.vote != r.vote)
            {
                if(this.vote == null || r.vote == null || !this.vote.equals(r.vote))
                {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public int hashCode()
    {
        int h_ = 5381;
        h_ = com.zeroc.IceInternal.HashUtil.hashAdd(h_, "::app::Vote");
        h_ = com.zeroc.IceInternal.HashUtil.hashAdd(h_, id);
        h_ = com.zeroc.IceInternal.HashUtil.hashAdd(h_, vote);
        return h_;
    }

    public Vote clone()
    {
        Vote c = null;
        try
        {
            c = (Vote)super.clone();
        }
        catch(CloneNotSupportedException ex)
        {
            assert false; // impossible
        }
        return c;
    }

    public void ice_writeMembers(com.zeroc.Ice.OutputStream ostr)
    {
        ostr.writeString(this.id);
        ostr.writeString(this.vote);
    }

    public void ice_readMembers(com.zeroc.Ice.InputStream istr)
    {
        this.id = istr.readString();
        this.vote = istr.readString();
    }

    static public void ice_write(com.zeroc.Ice.OutputStream ostr, Vote v)
    {
        if(v == null)
        {
            _nullMarshalValue.ice_writeMembers(ostr);
        }
        else
        {
            v.ice_writeMembers(ostr);
        }
    }

    static public Vote ice_read(com.zeroc.Ice.InputStream istr)
    {
        Vote v = new Vote();
        v.ice_readMembers(istr);
        return v;
    }

    static public void ice_write(com.zeroc.Ice.OutputStream ostr, int tag, java.util.Optional<Vote> v)
    {
        if(v != null && v.isPresent())
        {
            ice_write(ostr, tag, v.get());
        }
    }

    static public void ice_write(com.zeroc.Ice.OutputStream ostr, int tag, Vote v)
    {
        if(ostr.writeOptional(tag, com.zeroc.Ice.OptionalFormat.FSize))
        {
            int pos = ostr.startSize();
            ice_write(ostr, v);
            ostr.endSize(pos);
        }
    }

    static public java.util.Optional<Vote> ice_read(com.zeroc.Ice.InputStream istr, int tag)
    {
        if(istr.readOptional(tag, com.zeroc.Ice.OptionalFormat.FSize))
        {
            istr.skip(4);
            return java.util.Optional.of(Vote.ice_read(istr));
        }
        else
        {
            return java.util.Optional.empty();
        }
    }

    private static final Vote _nullMarshalValue = new Vote();

    /** @hidden */
    public static final long serialVersionUID = -8071942565755775193L;
}
