import { useState } from 'react'
import { ExternalLink, Mail, Pencil, Phone } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { EditProfileDialog } from './EditProfileDialog'
import type { MasterCv } from './types'

interface Props {
  cv: MasterCv
}

export function ProfileSection({ cv }: Props) {
  const [editOpen, setEditOpen] = useState(false)

  return (
    <section>
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">{cv.fullName}</h1>
          <p className="mt-0.5 text-sm text-slate-600">{cv.headline}</p>
        </div>
        <Button variant="outline" size="sm" onClick={() => setEditOpen(true)}>
          <Pencil className="h-4 w-4" />
          Edit
        </Button>
      </div>

      <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-500">
        <span className="flex items-center gap-1.5">
          <Mail className="h-3.5 w-3.5" />
          {cv.email}
        </span>
        {cv.phone && (
          <span className="flex items-center gap-1.5">
            <Phone className="h-3.5 w-3.5" />
            {cv.phone}
          </span>
        )}
        {cv.linkedInUrl && (
          <a
            href={cv.linkedInUrl}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 hover:text-slate-900"
          >
            <ExternalLink className="h-3.5 w-3.5" />
            LinkedIn
          </a>
        )}
        {cv.githubUrl && (
          <a
            href={cv.githubUrl}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 hover:text-slate-900"
          >
            <ExternalLink className="h-3.5 w-3.5" />
            GitHub
          </a>
        )}
      </div>

      {cv.summary && <p className="mt-4 max-w-3xl text-sm leading-relaxed text-slate-700">{cv.summary}</p>}

      <EditProfileDialog cv={cv} open={editOpen} onOpenChange={setEditOpen} />
    </section>
  )
}
