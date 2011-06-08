////////////////////////////////////////////////////////////////////////////////
// The following FIT Protocol software provided may be used with FIT protocol
// devices only and remains the copyrighted property of Dynastream Innovations Inc.
// The software is being provided on an "as-is" basis and as an accommodation,
// and therefore all warranties, representations, or guarantees of any kind
// (whether express, implied or statutory) including, without limitation,
// warranties of merchantability, non-infringement, or fitness for a particular
// purpose, are specifically disclaimed.
//
// Copyright 2008 Dynastream Innovations Inc.
////////////////////////////////////////////////////////////////////////////////
// ****WARNING****  This file is auto-generated!  Do NOT edit this file.
// Profile Version = 1.20Release
// Tag = $Name: AKW1_200 $
////////////////////////////////////////////////////////////////////////////////


#if !defined(FIT_DEVICE_SETTINGS_MESG_HPP)
#define FIT_DEVICE_SETTINGS_MESG_HPP

#include "fit_mesg.hpp"

namespace fit
{

class DeviceSettingsMesg : public Mesg
{
   public:
      DeviceSettingsMesg(void) : Mesg(Profile::MESG_DEVICE_SETTINGS)
      {
      }

      DeviceSettingsMesg(const Mesg &mesg) : Mesg(mesg)
      {
      }

      ///////////////////////////////////////////////////////////////////////
      // Returns utc_offset field
      // Comment: Offset from system time. Required to convert timestamp from system time to UTC.
      ///////////////////////////////////////////////////////////////////////
      FIT_UINT32 GetUtcOffset(void)
      {
         return GetFieldUINT32Value(1);
      }

      ///////////////////////////////////////////////////////////////////////
      // Set utc_offset field
      // Comment: Offset from system time. Required to convert timestamp from system time to UTC.
      ///////////////////////////////////////////////////////////////////////
      void SetUtcOffset(FIT_UINT32 utcOffset)
      {
         SetFieldUINT32Value(1, utcOffset);
      }

};

} // namespace fit

#endif // !defined(FIT_DEVICE_SETTINGS_MESG_HPP)
