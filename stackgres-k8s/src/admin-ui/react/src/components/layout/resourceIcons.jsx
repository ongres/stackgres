import {
  Cpu,
  Database,
  FileDatabase,
  Folders,
  GraphHub,
  History,
  ListClock,
  Network,
  Package,
  Puzzle,
  RouteArrowRight,
  ScrollText,
  SlidersHorizontal,
  TrafficCone,
  Waypoints,
} from 'lucide-react'

// Per-resource icons from the lucide set, matching the rest of the chrome
// (top bar, rail footer, cloud console) at stroke width 1.5. They replace the
// Vue console's own filled SVGs, which clashed with the outline icon style.
const withStroke = (Icon) => {
  const StrokedIcon = (props) => <Icon strokeWidth={1.5} {...props} />
  StrokedIcon.displayName = `Stroked(${Icon.displayName || Icon.name || 'Icon'})`
  return StrokedIcon
}

export const RESOURCE_ICONS = {
  namespaces: withStroke(Folders),
  sgclusters: withStroke(Database),
  sgshardedclusters: withStroke(Network),
  sgstreams: withStroke(Waypoints),
  sginstanceprofiles: withStroke(Cpu),
  configurations: withStroke(SlidersHorizontal),
  sgpgconfigs: withStroke(FileDatabase),
  sgpoolconfigs: withStroke(GraphHub),
  sgobjectstorages: withStroke(Package),
  sgscripts: withStroke(ScrollText),
  sgdistributedlogs: withStroke(ListClock),
  sgbackups: withStroke(History),
  sgdbops: withStroke(TrafficCone),
  applications: withStroke(Puzzle),
}

// Per-application icons, keyed by the application name the API reports;
// unlisted applications fall back to the generic applications icon.
export const APPLICATION_ICONS = {
  'babelfish-compass': withStroke(RouteArrowRight),
}
