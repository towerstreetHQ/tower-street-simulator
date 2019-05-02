package io.towerstreet.attacksimulator.scheduler

import play.api.inject.{SimpleModule, _}

class TaskModule extends SimpleModule(
  bind[ScoringScheduler].toSelf.eagerly(),
  bind[UrlTestScheduler].toSelf.eagerly()
)