// EventBusService.aidl
package com.lh.hermeseventbus;

// Declare any non-default types here with import statements

import com.lh.hermeseventbus.result.Request;
import com.lh.hermeseventbus.result.Responce;
interface EventBusService {
 Responce send(in Request request);
}
