package com.example

import eu.unicredit.reactive_aerospike.data.{AerospikeBinProto, AerospikeKey, AerospikeRecord}
import eu.unicredit.reactive_aerospike.model.Dao

case class Track(trackId: String, trackName: String)

object TrackDAO extends Dao[String, Track] {
  val namespace = "test"
  val setName = "trackdata"

  def getKeyDigest(obj: Track)= AerospikeKey(namespace, setName, obj.trackId).digest

  val objRead: (AerospikeKey[String], AerospikeRecord) => Track =
    (key: AerospikeKey[String], record: AerospikeRecord) =>
      Track(new String(key.digest), record.get("trackName"))

  val objWrite: Seq[AerospikeBinProto[Track, _]] = Seq(
    ("trackId", (t: Track) => t.trackId),
    ("trackName", (t: Track) => t.trackName))
}
