import * as React from "react"
import type { FieldError, UseFormRegisterReturn } from "react-hook-form"

import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

type FormFieldRenderProps = {
  id: string
  "aria-invalid"?: boolean
  "aria-describedby"?: string
} & Partial<UseFormRegisterReturn>

function FormField({
  label,
  hint,
  error,
  required,
  className,
  id: idProp,
  children,
  register,
}: {
  label?: React.ReactNode
  hint?: React.ReactNode
  error?: FieldError | string
  required?: boolean
  className?: string
  id?: string
  children: (props: FormFieldRenderProps) => React.ReactNode
  register?: UseFormRegisterReturn
}) {
  const reactId = React.useId()
  const id = idProp ?? reactId
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(" ") || undefined
  const errorMessage = typeof error === "string" ? error : error?.message

  const renderProps: FormFieldRenderProps = {
    id,
    "aria-invalid": Boolean(error) || undefined,
    "aria-describedby": describedBy,
    ...register,
  }

  return (
    <div data-slot="form-field" className={cn("flex flex-col gap-1.5", className)}>
      {label ? (
        <Label htmlFor={id} data-required={required || undefined}>
          {label}
          {required ? (
            <span className="text-destructive" aria-hidden>
              {" *"}
            </span>
          ) : null}
        </Label>
      ) : null}
      {children(renderProps)}
      {hint && !errorMessage ? (
        <p id={hintId} className="text-xs text-muted-foreground">
          {hint}
        </p>
      ) : null}
      {errorMessage ? (
        <p id={errorId} className="text-xs text-destructive" role="alert">
          {errorMessage}
        </p>
      ) : null}
    </div>
  )
}

export { FormField }
